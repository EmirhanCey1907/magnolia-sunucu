package com.oyun.magnolia.controller;

import com.oyun.magnolia.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Controller
public class OyunController {
    private Map<String, OyunDurumu> odalar = new ConcurrentHashMap<>();
    private Random rastgele = new Random();
    @Autowired private SimpMessagingTemplate mesajSistemi;

    @MessageMapping("/hamle")
    public void hamleYap(Hamle hamle) {
        String oda = hamle.getOdaAdi();
        if (oda == null || oda.isEmpty()) return;
        odalar.putIfAbsent(oda, new OyunDurumu());
        OyunDurumu oyun = odalar.get(oda);

        if ("BILGI_AL".equals(hamle.getIslem())) {
            mesajSistemi.convertAndSend("/oda/guncelleme/" + oda, oyun);
            return;
        }

        if ("KATIL".equals(hamle.getIslem())) {
            // SAYFA YENİLEME KONTROLÜ: Eğer oyuncu zaten odadaysa, bağlanmasına izin ver!
            if (oyun.getOyuncular().containsKey(hamle.getOyuncuAdi())) {
                oyun.setMesaj("🔄 " + hamle.getOyuncuAdi() + " geri bağlandı!");
            } else if (!oyun.isOyunBasladi() && oyun.getOyuncular().size() < 10) {
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
        else if ("BASLAT".equals(hamle.getIslem())) {
            if (hamle.getOyuncuAdi().equals(oyun.getKurucuAd())) {
                oyun.setOyunBasladi(true); yeniNesneOlustur(oyun);
                oyun.setMesaj("🚀 OYUN BAŞLADI!");
            }
        }
        else if ("TEKRAR".equals(hamle.getIslem())) {
            if (hamle.getOyuncuAdi().equals(oyun.getKurucuAd())) {
                oyun.setTurBitti(false);
                oyun.getOyuncular().values().forEach(o -> { o.setSkor(0); o.setKilitBitis(0); o.setHizliBasim(0); });
                yeniNesneOlustur(oyun);
                oyun.setMesaj("♻️ Yeni Maç Başladı!");
            }
        }
        else if ("CEK".equals(hamle.getIslem())) {
            if (!oyun.isOyunBasladi() || oyun.isTurBitti()) return;

            Oyuncu ceken = oyun.getOyuncular().get(hamle.getOyuncuAdi());
            if (ceken == null) return;
            long suAn = System.currentTimeMillis();

            // 1 SANİYELİK DOKUNULMAZLIK (MOLA): Yanlışlıkla yeni çıkan bombaya basmayı engeller!
            if (suAn < oyun.getTurBaslangicZamani() + 1000) return;

            if (oyun.isBombaAktif()) {
                ceken.setSkor(ceken.getSkor() - 2);
                oyun.setSonOlayTipi("BOMBA"); oyun.setSonOlayMesaji("💥 GÜM! (-2 Puan)");
                oyun.setOlayZamani(suAn); yeniNesneOlustur(oyun);
            } else {
                ceken.setSonBasim(suAn);

                int n = oyun.getOyuncular().size();
                double aci = ceken.getIndex() * (2 * Math.PI / n);
                oyun.setMagX(oyun.getMagX() + Math.cos(aci));
                oyun.setMagY(oyun.getMagY() + Math.sin(aci));

                // 5 BİRİM KURALI
                double mesafe = Math.sqrt(Math.pow(oyun.getMagX(), 2) + Math.pow(oyun.getMagY(), 2));
                if (mesafe >= 4.99) {
                    int artis = oyun.isAltinAktif() ? 2 : 1;
                    ceken.setSkor(ceken.getSkor() + artis);
                    if(oyun.isAltinAktif()) { oyun.setSonOlayTipi("ALTIN"); oyun.setSonOlayMesaji("🌟 ALTIN! (+2 Puan)"); }
                    oyun.setOlayZamani(suAn);

                    if (ceken.getSkor() >= 7) {
                        oyun.setTurBitti(true); oyun.setSonOlayTipi("KAZANDI");
                        oyun.setSonOlayMesaji("🏆 " + ceken.getAd().toUpperCase() + " KAZANDI!");
                    } else {
                        yeniNesneOlustur(oyun);
                    }
                }
            }
        }
        mesajSistemi.convertAndSend("/oda/guncelleme/" + oda, oyun);
    }

    private void yeniNesneOlustur(OyunDurumu oyun) {
        oyun.setMagX(0); oyun.setMagY(0);
        oyun.setBombaAktif(false); oyun.setAltinAktif(false);
        // Yeni tur başlangıç saatini kaydet (1 sn dokunulmazlık için)
        oyun.setTurBaslangicZamani(System.currentTimeMillis());

        int s = rastgele.nextInt(100);
        if (s < 20) { oyun.setBombaAktif(true); oyun.setNesneEmoji("💣"); }
        else {
            if (rastgele.nextInt(100) < 25) oyun.setAltinAktif(true);
            int t = rastgele.nextInt(3);
            if (t == 0) { oyun.setNesneEmoji("🍌"); } else if (t == 1) { oyun.setNesneEmoji("🍓"); } else { oyun.setNesneEmoji("🍫"); }
        }
    }
}