package com.oyun.magnolia.controller;

import com.oyun.magnolia.model.Hamle;
import com.oyun.magnolia.model.OyunDurumu;
import com.oyun.magnolia.model.Oyuncu;
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

    @Autowired
    private SimpMessagingTemplate mesajSistemi;

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
            if (!oyun.isOyunBasladi() && oyun.getOyuncular().size() < 10) {
                boolean karakterDolu = oyun.getOyuncular().values().stream()
                        .anyMatch(o -> o.getKarakter().equals(hamle.getKarakter()));
                if (karakterDolu) return;

                if (!oyun.getOyuncular().containsKey(hamle.getOyuncuAdi())) {
                    Oyuncu yeni = new Oyuncu();
                    yeni.setAd(hamle.getOyuncuAdi());
                    yeni.setKarakter(hamle.getKarakter());
                    yeni.setIndex(oyun.getOyuncular().size());

                    if (oyun.getOyuncular().isEmpty()) { oyun.setKurucuAd(yeni.getAd()); }

                    oyun.getOyuncular().put(yeni.getAd(), yeni);
                    oyun.setMesaj("👋 " + yeni.getAd() + " katıldı! (" + oyun.getOyuncular().size() + "/10)");
                }
            }
        }
        else if ("BASLAT".equals(hamle.getIslem())) {
            if (hamle.getOyuncuAdi().equals(oyun.getKurucuAd()) && oyun.getOyuncular().size() >= 2) {
                oyun.setOyunBasladi(true);
                yeniNesneOlustur(oyun);
                oyun.setMesaj("🚀 Oyun Başladı! Butonlara Asılın!");
            }
        }
        else if ("TEKRAR".equals(hamle.getIslem())) {
            if (hamle.getOyuncuAdi().equals(oyun.getKurucuAd())) {
                oyun.setTurBitti(false);
                for (Oyuncu o : oyun.getOyuncular().values()) {
                    o.setSkor(0); o.setKilitBitis(0); o.setHizliBasim(0);
                }
                yeniNesneOlustur(oyun);
                oyun.setMesaj("♻️ Yeni Maç Başladı! Herkes Sıfırdan!");
            }
        }
        else if ("CEK".equals(hamle.getIslem())) {
            if (!oyun.isOyunBasladi() || oyun.isTurBitti()) return;

            Oyuncu ceken = oyun.getOyuncular().get(hamle.getOyuncuAdi());
            if (ceken == null) return;
            long suAn = System.currentTimeMillis();

            if (oyun.isBombaAktif()) {
                if (suAn < ceken.getKilitBitis()) return;
                ceken.setSkor(ceken.getSkor() - 2);

                oyun.setSonOlayTipi("BOMBA");
                oyun.setSonOlayMesaji("💥 " + ceken.getAd() + " PATLADI! (-2 Puan)");
                oyun.setOlayZamani(System.currentTimeMillis());
                oyun.setMesaj("GÜM! " + ceken.getAd() + " tuzağa düştü! (-2)");

                yeniNesneOlustur(oyun);
            }
            else {
                if (suAn < ceken.getKilitBitis()) return;
                if (suAn - ceken.getSonBasim() < 300) {
                    ceken.setHizliBasim(ceken.getHizliBasim() + 1);
                    if (ceken.getHizliBasim() >= 3) {
                        ceken.setKilitBitis(suAn + 2000);
                        ceken.setHizliBasim(0);
                        oyun.setMesaj("🔥 " + ceken.getAd() + " motoru yaktı!");
                        mesajSistemi.convertAndSend("/oda/guncelleme/" + oda, oyun);
                        return;
                    }
                } else { ceken.setHizliBasim(1); }
                ceken.setSonBasim(suAn);

                int n = oyun.getOyuncular().size();
                double aci = ceken.getIndex() * (2 * Math.PI / n);
                oyun.setMagX(oyun.getMagX() + Math.cos(aci));
                oyun.setMagY(oyun.getMagY() + Math.sin(aci));

                double mesafe = Math.sqrt((oyun.getMagX() * oyun.getMagX()) + (oyun.getMagY() * oyun.getMagY()));
                if (mesafe >= 10.0) {
                    if (oyun.isAltinAktif()) {
                        ceken.setSkor(ceken.getSkor() + 2);
                        oyun.setSonOlayTipi("ALTIN");
                        oyun.setSonOlayMesaji("🌟 " + ceken.getAd() + " GİZLİ ALTINI KAPTI! (+2)");
                        oyun.setOlayZamani(System.currentTimeMillis());
                        oyun.setMesaj(ceken.getAd() + " altını buldu!");
                    } else {
                        ceken.setSkor(ceken.getSkor() + 1);
                        oyun.setMesaj("🍨 " + ceken.getAd() + " turu aldı!");
                    }

                    if (ceken.getSkor() >= 7) {
                        oyun.setTurBitti(true);
                        oyun.setSonOlayTipi("KAZANDI");
                        oyun.setSonOlayMesaji("🏆 " + ceken.getAd() + " ŞAMPİYON! 🏆");
                        oyun.setOlayZamani(System.currentTimeMillis());
                        oyun.setMesaj(ceken.getAd() + " oyunu kazandı!");
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

        int sans = rastgele.nextInt(100);
        if (sans < 20) {
            oyun.setBombaAktif(true);
            oyun.setGosterilenNesne("💣 TUZAK BOMBA! (-2 Puan)");
            oyun.setNesneEmoji("💣☠️"); // Bomba görseli
        } else {
            if (rastgele.nextInt(100) < 25) oyun.setAltinAktif(true);

            int tur = rastgele.nextInt(3);
            if (tur == 0) {
                oyun.setGosterilenNesne("🍌 Muzlu Magnolia");
                oyun.setNesneEmoji("🍌🍨"); // Muzlu Magnolia Görseli
            } else if (tur == 1) {
                oyun.setGosterilenNesne("🍓 Çilekli Magnolia");
                oyun.setNesneEmoji("🍓🍨"); // Çilekli Magnolia Görseli
            } else {
                oyun.setGosterilenNesne("🍫 Çikolatalı Magnolia");
                oyun.setNesneEmoji("🍫🍨"); // Çikolatalı Magnolia Görseli
            }
        }
    }
}