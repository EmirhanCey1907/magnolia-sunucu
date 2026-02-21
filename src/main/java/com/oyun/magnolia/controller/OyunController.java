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
            if (oyun.getOyuncular().containsKey(hamle.getOyuncuAdi())) {
                oyun.setMesaj("🔄 " + hamle.getOyuncuAdi() + " oyuna döndü!");
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
                oyun.setOyunBasladi(true); yeniNesneOlustur(oyun, System.currentTimeMillis());
                oyun.setMesaj("🚀 İLK 7 YAPAN KAZANIR!");
            }
        }
        else if ("TEKRAR".equals(hamle.getIslem())) {
            if (hamle.getOyuncuAdi().equals(oyun.getKurucuAd())) {
                oyun.setTurBitti(false);
                oyun.getOyuncular().values().forEach(o -> { o.setSkor(0); o.setKilitBitis(0); o.setHizliBasim(0); });
                yeniNesneOlustur(oyun, System.currentTimeMillis());
                oyun.setMesaj("♻️ YENİ MAÇ BAŞLADI!");
            }
        }
        else if ("CEK".equals(hamle.getIslem())) {
            if (!oyun.isOyunBasladi() || oyun.isTurBitti()) return;
            long suAn = System.currentTimeMillis();

            // YANLIŞLIKLA BOMBAYA BASMAMAK İÇİN HER TUR BAŞI 0.5 SN KORUMA (DOKUNULMAZLIK)
            if (suAn < oyun.getTurBaslangicZamani() + 500) return;

            Oyuncu ceken = oyun.getOyuncular().get(hamle.getOyuncuAdi());
            if (ceken == null || suAn < ceken.getKilitBitis()) return;

            // CEZA SİSTEMİ GERİ DÖNDÜ: 300ms içinde 3 hızlı basışa 2 saniye kilit!
            if (suAn - ceken.getSonBasim() < 300) {
                ceken.setHizliBasim(ceken.getHizliBasim() + 1);
                if (ceken.getHizliBasim() >= 3) {
                    ceken.setKilitBitis(suAn + 2000);
                    ceken.setHizliBasim(0);
                    oyun.setMesaj("🔥 " + ceken.getAd() + " MOTORU YAKTI! (2sn Ceza)");
                    mesajSistemi.convertAndSend("/oda/guncelleme/" + oda, oyun);
                    return;
                }
            } else { ceken.setHizliBasim(1); }
            ceken.setSonBasim(suAn);

            if (oyun.isBombaAktif()) {
                ceken.setSkor(ceken.getSkor() - 2);
                oyun.setSonOlayTipi("BOMBA"); oyun.setSonOlayMesaji("💥 GÜM! (-2 Puan)");
                oyun.setOlayZamani(suAn);
                yeniNesneOlustur(oyun, suAn);
            } else {
                int n = oyun.getOyuncular().size();
                double aci = ceken.getIndex() * (2 * Math.PI / n);
                oyun.setMagX(oyun.getMagX() + Math.cos(aci));
                oyun.setMagY(oyun.getMagY() + Math.sin(aci));

                // TAM 5 TIK KURALI
                double mesafe = Math.sqrt(Math.pow(oyun.getMagX(), 2) + Math.pow(oyun.getMagY(), 2));
                if (mesafe >= 4.99) {
                    if(oyun.isAltinAktif()) {
                        ceken.setSkor(ceken.getSkor() + 2);
                        oyun.setSonOlayTipi("ALTIN"); oyun.setSonOlayMesaji("🌟 GİZLİ ALTIN! (+2)");
                    } else {
                        ceken.setSkor(ceken.getSkor() + 1);
                        oyun.setSonOlayTipi("NORMAL"); oyun.setSonOlayMesaji("🍨 +1 PUAN!");
                    }
                    oyun.setOlayZamani(suAn);

                    // 7 PUANDA OYUN BİTER
                    if (ceken.getSkor() >= 7) {
                        oyun.setTurBitti(true); oyun.setSonOlayTipi("KAZANDI");
                        oyun.setSonOlayMesaji("🏆 " + ceken.getAd().toUpperCase() + " ŞAMPİYON!");
                    } else {
                        yeniNesneOlustur(oyun, suAn);
                    }
                }
            }
        }
        mesajSistemi.convertAndSend("/oda/guncelleme/" + oda, oyun);
    }

    private void yeniNesneOlustur(OyunDurumu oyun, long suAn) {
        oyun.setMagX(0); oyun.setMagY(0);
        oyun.setBombaAktif(false); oyun.setAltinAktif(false);
        oyun.setTurBaslangicZamani(suAn); // Yeni nesne geldiğinde 0.5s mola başlar

        int s = rastgele.nextInt(100);
        if (s < 20) { oyun.setBombaAktif(true); oyun.setNesneEmoji("💣"); }
        else {
            if (rastgele.nextInt(100) < 25) oyun.setAltinAktif(true);
            int t = rastgele.nextInt(3);
            if (t == 0) { oyun.setNesneEmoji("🍌"); } else if (t == 1) { oyun.setNesneEmoji("🍓"); } else { oyun.setNesneEmoji("🍫"); }
        }
    }
}