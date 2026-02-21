package com.oyun.magnolia.model;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class OyunDurumu {
    private String odaAdi, kurucuAd, mesaj = "Lobi: Rakipler Bekleniyor...", sonOlayTipi = "", sonOlayMesaji = "", gosterilenNesne = "Klasik", nesneEmoji = "🍨";
    private Map<String, Oyuncu> oyuncular = new ConcurrentHashMap<>();
    private List<String> sohbet = new CopyOnWriteArrayList<>();
    private boolean oyunBasladi = false, turBitti = false, bombaAktif = false, altinAktif = false, buzAktif = false;
    private long olayZamani = 0, turBaslangicZamani = 0;
    private String aktifSahip = null;
    private int aktifMesafe = 0;

    // EFSANEVİ SENKRONİZASYON (Sunucunun tam saati)
    public long getSunucuZamani() { return System.currentTimeMillis(); }

    public String getOdaAdi() { return odaAdi; } public void setOdaAdi(String odaAdi) { this.odaAdi = odaAdi; }
    public String getKurucuAd() { return kurucuAd; } public void setKurucuAd(String kurucuAd) { this.kurucuAd = kurucuAd; }
    public Map<String, Oyuncu> getOyuncular() { return oyuncular; } public void setOyuncular(Map<String, Oyuncu> oyuncular) { this.oyuncular = oyuncular; }
    public List<String> getSohbet() { return sohbet; } public void setSohbet(List<String> sohbet) { this.sohbet = sohbet; }
    public boolean isOyunBasladi() { return oyunBasladi; } public void setOyunBasladi(boolean oyunBasladi) { this.oyunBasladi = oyunBasladi; }
    public boolean isTurBitti() { return turBitti; } public void setTurBitti(boolean turBitti) { this.turBitti = turBitti; }
    public String getMesaj() { return mesaj; } public void setMesaj(String mesaj) { this.mesaj = mesaj; }
    public String getSonOlayTipi() { return sonOlayTipi; } public void setSonOlayTipi(String sonOlayTipi) { this.sonOlayTipi = sonOlayTipi; }
    public String getSonOlayMesaji() { return sonOlayMesaji; } public void setSonOlayMesaji(String sonOlayMesaji) { this.sonOlayMesaji = sonOlayMesaji; }
    public long getOlayZamani() { return olayZamani; } public void setOlayZamani(long olayZamani) { this.olayZamani = olayZamani; }
    public long getTurBaslangicZamani() { return turBaslangicZamani; } public void setTurBaslangicZamani(long turBaslangicZamani) { this.turBaslangicZamani = turBaslangicZamani; }
    public String getAktifSahip() { return aktifSahip; } public void setAktifSahip(String aktifSahip) { this.aktifSahip = aktifSahip; }
    public int getAktifMesafe() { return aktifMesafe; } public void setAktifMesafe(int aktifMesafe) { this.aktifMesafe = aktifMesafe; }
    public String getGosterilenNesne() { return gosterilenNesne; } public void setGosterilenNesne(String gosterilenNesne) { this.gosterilenNesne = gosterilenNesne; }
    public String getNesneEmoji() { return nesneEmoji; } public void setNesneEmoji(String nesneEmoji) { this.nesneEmoji = nesneEmoji; }
    public boolean isBombaAktif() { return bombaAktif; } public void setBombaAktif(boolean bombaAktif) { this.bombaAktif = bombaAktif; }
    public boolean isAltinAktif() { return altinAktif; } public void setAltinAktif(boolean altinAktif) { this.altinAktif = altinAktif; }
    public boolean isBuzAktif() { return buzAktif; } public void setBuzAktif(boolean buzAktif) { this.buzAktif = buzAktif; }
}