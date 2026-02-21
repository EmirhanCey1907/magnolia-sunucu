package com.oyun.magnolia.model;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
public class OyunDurumu {
    private String odaAdi, kurucuAd, mesaj = "Lobi: Rakipler Bekleniyor...", sonOlayTipi = "", sonOlayMesaji = "", gosterilenNesne = "Klasik", nesneEmoji = "🍨";
    private Map<String, Oyuncu> oyuncular = new ConcurrentHashMap<>();
    private boolean oyunBasladi = false, turBitti = false, bombaAktif = false, altinAktif = false;
    private long olayZamani = 0, turBaslangicZamani = 0;
    private double magX = 0, magY = 0;

    public String getOdaAdi() { return odaAdi; } public void setOdaAdi(String odaAdi) { this.odaAdi = odaAdi; }
    public String getKurucuAd() { return kurucuAd; } public void setKurucuAd(String kurucuAd) { this.kurucuAd = kurucuAd; }
    public Map<String, Oyuncu> getOyuncular() { return oyuncular; } public void setOyuncular(Map<String, Oyuncu> oyuncular) { this.oyuncular = oyuncular; }
    public boolean isOyunBasladi() { return oyunBasladi; } public void setOyunBasladi(boolean oyunBasladi) { this.oyunBasladi = oyunBasladi; }
    public boolean isTurBitti() { return turBitti; } public void setTurBitti(boolean turBitti) { this.turBitti = turBitti; }
    public String getMesaj() { return mesaj; } public void setMesaj(String mesaj) { this.mesaj = mesaj; }
    public String getSonOlayTipi() { return sonOlayTipi; } public void setSonOlayTipi(String sonOlayTipi) { this.sonOlayTipi = sonOlayTipi; }
    public String getSonOlayMesaji() { return sonOlayMesaji; } public void setSonOlayMesaji(String sonOlayMesaji) { this.sonOlayMesaji = sonOlayMesaji; }
    public long getOlayZamani() { return olayZamani; } public void setOlayZamani(long olayZamani) { this.olayZamani = olayZamani; }
    public long getTurBaslangicZamani() { return turBaslangicZamani; } public void setTurBaslangicZamani(long turBaslangicZamani) { this.turBaslangicZamani = turBaslangicZamani; }
    public double getMagX() { return magX; } public void setMagX(double magX) { this.magX = magX; }
    public double getMagY() { return magY; } public void setMagY(double magY) { this.magY = magY; }
    public String getGosterilenNesne() { return gosterilenNesne; } public void setGosterilenNesne(String gosterilenNesne) { this.gosterilenNesne = gosterilenNesne; }
    public String getNesneEmoji() { return nesneEmoji; } public void setNesneEmoji(String nesneEmoji) { this.nesneEmoji = nesneEmoji; }
    public boolean isBombaAktif() { return bombaAktif; } public void setBombaAktif(boolean bombaAktif) { this.bombaAktif = bombaAktif; }
    public boolean isAltinAktif() { return altinAktif; } public void setAltinAktif(boolean altinAktif) { this.altinAktif = altinAktif; }
}