package com.oyun.magnolia.controller;

import com.oyun.magnolia.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Controller
public class OyunController {
    private Map<String, OyunDurumu> odalar = new ConcurrentHashMap<>();
    private Random rastgele = new Random();
    private ScheduledExecutorService zamanlayici = Executors.newScheduledThreadPool(5);

    @Autowired private SimpMessagingTemplate mesajSistemi;

    @MessageMapping("/hamle")
    public void hamleYap(Hamle hamle) {
        String oda = hamle.getOdaAdi();
        if (oda == null || oda.isEmpty()) return;

        if ("KUR".equals(hamle.getIslem())) {
            odalar.putIfAbsent(oda, new OyunDurumu());
            odalar.get(oda).setOdaAdi(oda);
            mesajSistemi.convertAndSend("/oda/guncelleme/" + oda, odalar.get(oda));
            return;
        }

        if (!odalar.containsKey(oda)) {
            OyunDurumu hata = new OyunDurumu();
            hata.setMesaj("HATA_ODA_YOK");
            mesajSistemi.convertAndSend("/oda/guncelleme/" + oda, hata);
            return;
        }

        OyunDurumu oyun = odalar.get(oda);

        if ("BILGI_AL".equals(hamle.getIslem())) {
            mesajSistemi.convertAndSend("/oda/guncelleme/" + oda, oyun); return;
        }

        // SOHBET SİSTEMİ
        if ("SOHBET".equals(hamle.getIslem())) {
            if (hamle.getMetin() != null && !hamle.getMetin().trim().isEmpty()) {
                String sohbetMesaji = hamle.getKarakter() + " <b>" + hamle.getOyuncuAdi() + "</b>: " + hamle.getMetin().trim();
                oyun.getSohbet().add(sohbetMesaji);
                if (oyun.getSohbet().size() > 20) oyun.getSohbet().remove(0); // Sadece son 20 mesaj
                mesajSistemi.convertAndSend("/oda/guncelleme/" + oda, oyun);
            }
            return;
        }

        if ("AYRIL".equals(hamle.getIslem())) {
            oyun.getOyuncular().remove(hamle.getOyuncuAdi());

            if (oyun.getOyuncular().isEmpty()) {
                odalar.remove(oda);
                return;
            }

            if (oyun.getKurucuAd() != null && oyun.getKurucuAd().equals(hamle.getOyuncuAdi())) {
                oyun.setKurucuAd(oyun.getOyuncular().keySet().iterator().next());
            }
            oyun.setMesaj("🚪 " + hamle.getOyuncuAdi() + " ayrıldı.");
            int index = 0; for (Oyuncu o : oyun.getOyuncular().values()) o.setIndex(index++);

            if (oyun.isOyunBasladi() && !oyun.isTurBitti() && oyun.getOyuncular().size() < 2) {
                oyun.setTurBitti(true);
                oyun.setSonOlayTipi("IPTAL");
                oyun.setSonOlayMesaji("🛑 RAKİP KAÇTI!");
                oyun.setOlayZamani(System.currentTimeMillis());
            }
            mesajSistemi.convertAndSend("/oda/guncelleme/" + oda, oyun); return;
        }

        if ("KATIL".equals(hamle.getIslem())) {
            if (oyun.getOyuncular().containsKey(hamle.getOyuncuAdi())) {
                oyun.setMesaj("🔄 " + hamle.getOyuncuAdi() + " oyuna döndü!");
            } else if (!oyun.isOyunBasladi() || oyun.isTurBitti()) {
                if (oyun.getOyuncular().size() < 10) {
                    boolean dolu = oyun.getOyuncular().values().stream().anyMatch(o -> o.getKarakter().equals(hamle.getKarakter()));
                    if (dolu) return;
                    Oyuncu yeni = new Oyuncu();
                    yeni.setAd(hamle.getOyuncuAdi()); yeni.setKarakter(hamle.getKarakter());
                    yeni.setIndex(oyun.getOyuncular().size());
                    if (oyun.getOyuncular().isEmpty()) oyun.setKurucuAd(yeni.getAd());
                    oyun.getOyuncular().put(yeni.getAd(), yeni);
                    oyun.setMesaj("👋 " + yeni.getAd() + " katıldı!");
                }
            }
        }
        else if ("BASLAT".equals(hamle.getIslem())) {
            if (hamle.getOyuncuAdi().equals(oyun.getKurucuAd())) {
                if (oyun.getOyuncular().size() < 2) {
                    oyun.setMesaj("❌ Başlamak için en az 2 kişi lazım!");
                } else {
                    oyun.setOyunBasladi(true); oyun.setTurBitti(false);
                    yeniNesneOlustur(oyun, System.currentTimeMillis(), oda);
                    oyun.setMesaj("🚀 İLK 7 YAPAN KAZANIR!");
                }
            }
        }
        else if ("TEKRAR".equals(hamle.getIslem())) {
            if (hamle.getOyuncuAdi().equals(oyun.getKurucuAd())) {
                if (oyun.getOyuncular().size() < 2) {
                    oyun.setMesaj("❌ Başlamak için en az 2 kişi lazım!");
                } else {
                    oyun.setTurBitti(false);
                    oyun.getOyuncular().values().forEach(o -> { o.setSkor(0); o.setKilitBitis(0); o.setHizliBasim(0); o.setDonduruldu(false); });
                    yeniNesneOlustur(oyun, System.currentTimeMillis(), oda);
                    oyun.setMesaj("♻️ YENİ MAÇ BAŞLADI!");
                }
            }
        }
        else if ("CEK".equals(hamle.getIslem())) {
            if (!oyun.isOyunBasladi() || oyun.isTurBitti()) return;
            long suAn = System.currentTimeMillis();

            if (suAn < oyun.getTurBaslangicZamani() + 500) return;

            Oyuncu ceken = oyun.getOyuncular().get(hamle.getOyuncuAdi());
            if (ceken == null || suAn < ceken.getKilitBitis()) return;

            if (suAn - ceken.getSonBasim() < 300) {
                ceken.setHizliBasim(ceken.getHizliBasim() + 1);
                if (ceken.getHizliBasim() >= 3) {
                    ceken.setKilitBitis(suAn + 1000); // CEZA ARTIK 1 SANİYE
                    ceken.setDonduruldu(false); ceken.setHizliBasim(0);
                    oyun.setMesaj("🔥 " + ceken.getAd() + " SPAM YAPTI! (1sn Ceza)");
                    mesajSistemi.convertAndSend("/oda/guncelleme/" + oda, oyun); return;
                }
            } else { ceken.setHizliBasim(1); }
            ceken.setSonBasim(suAn);

            if (oyun.isBombaAktif()) {
                ceken.setSkor(ceken.getSkor() - 2);
                oyun.setSonOlayTipi("BOMBA"); oyun.setSonOlayMesaji("💥 GÜM! (-2 Puan)");
                yeniNesneOlustur(oyun, suAn, oda);
            }
            else if (oyun.isBuzAktif()) {
                for (Oyuncu o : oyun.getOyuncular().values()) {
                    if (!o.getAd().equals(ceken.getAd())) {
                        o.setKilitBitis(suAn + 1500); o.setDonduruldu(true);
                    }
                }
                oyun.setSonOlayTipi("BUZ"); oyun.setSonOlayMesaji("🧊 " + ceken.getAd().toUpperCase() + " DONDURDU!");
                yeniNesneOlustur(oyun, suAn, oda);
            }
            else {
                if (oyun.getAktifSahip() == null) {
                    oyun.setAktifSahip(ceken.getAd()); oyun.setAktifMesafe(1);
                } else if (oyun.getAktifSahip().equals(ceken.getAd())) {
                    oyun.setAktifMesafe(oyun.getAktifMesafe() + 1);
                } else {
                    oyun.setAktifMesafe(oyun.getAktifMesafe() - 1);
                    if (oyun.getAktifMesafe() == 0) oyun.setAktifSahip(null);
                }

                if (oyun.getAktifMesafe() >= 5) {
                    int artis = oyun.isAltinAktif() ? 2 : 1;
                    ceken.setSkor(ceken.getSkor() + artis);
                    if(oyun.isAltinAktif()) { oyun.setSonOlayTipi("ALTIN"); oyun.setSonOlayMesaji("🌟 GİZLİ ALTIN! (+2)"); }
                    else { oyun.setSonOlayTipi("NORMAL"); oyun.setSonOlayMesaji("🍨 +1 PUAN!"); }

                    oyun.setOlayZamani(suAn);
                    if (ceken.getSkor() >= 7) {
                        oyun.setTurBitti(true); oyun.setSonOlayTipi("KAZANDI");
                        oyun.setSonOlayMesaji("🏆 " + ceken.getAd().toUpperCase() + " ŞAMPİYON!");
                    } else {
                        yeniNesneOlustur(oyun, suAn, oda);
                    }
                }
            }
        }
        mesajSistemi.convertAndSend("/oda/guncelleme/" + oda, oyun);
    }

    private void yeniNesneOlustur(OyunDurumu oyun, long suAn, String oda) {
        oyun.setAktifSahip(null); oyun.setAktifMesafe(0);
        oyun.setBombaAktif(false); oyun.setAltinAktif(false); oyun.setBuzAktif(false);
        oyun.setTurBaslangicZamani(suAn); oyun.setOlayZamani(suAn);

        int s = rastgele.nextInt(100);
        if (s < 15) {
            oyun.setBombaAktif(true); oyun.setNesneEmoji("💣");
        } else if (s < 30) {
            oyun.setBuzAktif(true); oyun.setNesneEmoji("🧊");
        } else {
            if (rastgele.nextInt(100) < 25) oyun.setAltinAktif(true);
            int t = rastgele.nextInt(3);
            if (t == 0) { oyun.setNesneEmoji("🍌"); } else if (t == 1) { oyun.setNesneEmoji("🍓"); } else { oyun.setNesneEmoji("🍫"); }
        }

        if (oyun.isBombaAktif() || oyun.isBuzAktif()) {
            zamanlayici.schedule(() -> {
                if (oyun.getTurBaslangicZamani() == suAn && oyun.isOyunBasladi() && !oyun.isTurBitti()) {
                    oyun.setSonOlayTipi("ZAMAN_DOLDU");
                    oyun.setSonOlayMesaji("⏳ KİMSE DOKUNMADI!");
                    oyun.setOlayZamani(System.currentTimeMillis());
                    yeniNesneOlustur(oyun, System.currentTimeMillis(), oda);
                    mesajSistemi.convertAndSend("/oda/guncelleme/" + oda, oyun);
                }
            }, 3500, TimeUnit.MILLISECONDS);
        }
    }
}