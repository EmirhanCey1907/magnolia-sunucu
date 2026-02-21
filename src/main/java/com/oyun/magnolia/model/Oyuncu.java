package com.oyun.magnolia.model;
public class Oyuncu {
    private String ad, karakter;
    private int skor = 0, index = 0, hizliBasim = 0;
    private long kilitBitis = 0, sonBasim = 0;
    private boolean donduruldu = false;

    public String getAd() { return ad; } public void setAd(String ad) { this.ad = ad; }
    public String getKarakter() { return karakter; } public void setKarakter(String karakter) { this.karakter = karakter; }
    public int getSkor() { return skor; } public void setSkor(int skor) { this.skor = skor; }
    public int getIndex() { return index; } public void setIndex(int index) { this.index = index; }
    public long getKilitBitis() { return kilitBitis; } public void setKilitBitis(long kilitBitis) { this.kilitBitis = kilitBitis; }
    public int getHizliBasim() { return hizliBasim; } public void setHizliBasim(int hizliBasim) { this.hizliBasim = hizliBasim; }
    public long getSonBasim() { return sonBasim; } public void setSonBasim(long sonBasim) { this.sonBasim = sonBasim; }
    public boolean isDonduruldu() { return donduruldu; } public void setDonduruldu(boolean donduruldu) { this.donduruldu = donduruldu; }
}