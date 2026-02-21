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
                boolean dolu = oyun.getOyuncular().values().stream()
                        .anyMatch(o -> o.getKarakter().equals(hamle.getKarakter()));
                if (dolu) return;

                if (!oyun.getOyuncular().containsKey(hamle.getOyuncuAdi())) {
                    Oyuncu yeni = new Oyuncu();
                    yeni.setAd(hamle.getOyuncuAdi());
                    yeni.setKarakter(hamle.getKarakter());
                    yeni.setIndex(oyun.getOyuncular().size());

                    if (oyun.getOyuncular().isEmpty()) { oyun.setKurucuAd(yeni.getAd()); }
                    oyun.getOyuncular().put(yeni.getAd(), yeni);
                    oyun.setMesaj("👋 " + yeni.getAd() + " katıldı!");
                }
            }
        }
        else if ("BASLAT".equals(hamle.getIslem())) {
            if (hamle.getOyuncuAdi().equals(oyun.getKurucuAd())) {
                oyun.setOyunBasladi(true);
                yeniNesneOlustur(oyun);
                oyun.setMesaj("🚀 NET 5 KERE ÇEKEN ALIR!");
            }
        }
        else if ("TEKRAR".equals(hamle.getIslem())) {
            if (hamle.getOyuncuAdi().equals(oyun.getKurucuAd())) {
                oyun.setTurBitti(false);
                oyun.getOyuncular().values().forEach(o -> {
                    o.setSkor(0);
                    o.setKilitBitis(0);
                });
                yeniNesneOlustur(oyun);
                oyun.setMesaj("♻️ Yeni Maç Başladı!");
            }
        }
        else if ("CEK".equals(hamle.getIslem())) {
            if (!oyun.isOyunBasladi() || oyun.isTurBitti()) return;

            Oyuncu ceken = oyun.getOyuncular().get(hamle.getOyuncuAdi());
            if (ceken == null) return;
            long suAn = System.currentTimeMillis();

            if (oyun.isBombaAktif()) {
                ceken.setSkor(ceken.getSkor() - 2);
                oyun.setSonOlayTipi("BOMBA");
                oyun.setSonOlayMesaji("💥 GÜM! (-2)");
                oyun.setOlayZamani(suAn);
                yeniNesneOlustur(oyun);
            }
            else {
                // SİNİR BOZUCU CEZA SİSTEMİ KALDIRILDI!
                // Sadece saniyede 12 tık'tan (80ms) hızlı basan makroları engelle.
                // İnsanların hızlı basışlarını (tak tak tak) asla yutmaz.
                if (suAn - ceken.getSonBasim() < 80) {
                    return;
                }
                ceken.setSonBasim(suAn);

                // Matematik: Her tık Magnolia'yı tam 1 birim çeker
                int n = oyun.getOyuncular().size();
                double aci = ceken.getIndex() * (2 * Math.PI / n);
                oyun.setMagX(oyun.getMagX() + Math.cos(aci));
                oyun.setMagY(oyun.getMagY() + Math.sin(aci));

                // 5 TIK KURALI (Net olarak 4.99'dan sonrası)
                double mesafe = Math.sqrt(Math.pow(oyun.getMagX(), 2) + Math.pow(oyun.getMagY(), 2));
                if (mesafe >= 4.99) {
                    int artis = oyun.isAltinAktif() ? 2 : 1;
                    ceken.setSkor(ceken.getSkor() + artis);

                    if(oyun.isAltinAktif()) {
                        oyun.setSonOlayTipi("ALTIN");
                        oyun.setSonOlayMesaji("🌟 ALTIN! (+2)");
                    }
                    oyun.setOlayZamani(suAn);

                    // 7 PUAN KURALI (ŞAMPİYONLUK)
                    if (ceken.getSkor() >= 7) {
                        oyun.setTurBitti(true);
                        oyun.setSonOlayTipi("KAZANDI");
                        oyun.setSonOlayMesaji("🏆 " + ceken.getAd().toUpperCase() + " KAZANDI!");
                    } else {
                        yeniNesneOlustur(oyun); // Nesneyi ve pozisyonu sıfırla
                    }
                }
            }
        }
        mesajSistemi.convertAndSend("/oda/guncelleme/" + oda, oyun);
    }

    private void yeniNesneOlustur(OyunDurumu oyun) {
        oyun.setMagX(0); oyun.setMagY(0); // Merkezi sıfırla
        oyun.setBombaAktif(false); oyun.setAltinAktif(false);
        int s = rastgele.nextInt(100);
        if (s < 20) {
            oyun.setBombaAktif(true);
            oyun.setNesneEmoji("💣☠️");
        } else {
            if (rastgele.nextInt(100) < 25) oyun.setAltinAktif(true);
            int t = rastgele.nextInt(3);
            if (t == 0) { oyun.setNesneEmoji("🍌🍨"); }
            else if (t == 1) { oyun.setNesneEmoji("🍓🍨"); }
            else { oyun.setNesneEmoji("🍫🍨"); }
        }
    }
}