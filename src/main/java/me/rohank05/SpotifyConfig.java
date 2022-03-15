package me.rohank05;

import com.neovisionaries.i18n.CountryCode;

public class SpotifyConfig {
    public String clientId;
    public String clientSecret;
    public CountryCode countryCode = CountryCode.US;

    public SpotifyConfig(String clientId, String clientSecret){
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }
}
