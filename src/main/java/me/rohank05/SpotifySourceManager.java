package me.rohank05;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.track.AudioItem;
import com.sedmelluq.discord.lavaplayer.track.AudioReference;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import org.apache.hc.core5.http.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.specification.Track;
import se.michaelthelin.spotify.requests.authorization.client_credentials.ClientCredentialsRequest;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SpotifySourceManager implements AudioSourceManager {

    private final Pattern SPOTIFY_URL_PATTERN = Pattern.compile("(https?://)?(www\\.)?open\\.spotify\\.com/(user/[a-zA-Z0-9-_]+/)?(?<type>track|album|playlist|artist)/(?<identifier>[a-zA-Z0-9-_]+)");
    public static final int MAX_PAGE_ITEMS = 100;
    private static final Logger logger = LoggerFactory.getLogger(SpotifySourceManager.class);
    public SpotifyApi spotify;
    private final SpotifyConfig config;
    private final ClientCredentialsRequest clientCredentialsRequest;
    private final AudioPlayerManager audioPlayerManager;

    public SpotifySourceManager(SpotifyConfig spotifyConfig, AudioPlayerManager audioPlayerManager){
            this.config = spotifyConfig;
            this.audioPlayerManager = audioPlayerManager;
            this.spotify = new SpotifyApi.Builder().setClientId(config.clientId).setClientSecret(config.clientSecret).build();
            this.clientCredentialsRequest = this.spotify.clientCredentials().build();
            Thread thread = new Thread(() -> {
                try{
                    while(true){
                        try{
                            var clientCredentials = this.clientCredentialsRequest.execute();
                            this.spotify.setAccessToken(clientCredentials.getAccessToken());
                            Thread.sleep(clientCredentials.getExpiresIn() * 1000);
                        }
                        catch(IOException | SpotifyWebApiException | ParseException e){
                            logger.error("Failed to update the spotify access token. Retrying in 1 minute ", e);
                            Thread.sleep(60 * 1000);
                        }
                    }
                }catch (Exception e){
                    logger.error("Failed to update spotify Token", e);
                }
            });
            thread.setDaemon(true);
            thread.start();
    }

    public AudioPlayerManager getAudioPlayerManager() {
        return audioPlayerManager;
    }



    @Override
    public String getSourceName() {
        return "spotify";
    }

    @Override
    public AudioItem loadItem(AudioPlayerManager manager, AudioReference reference) {
        Matcher matcher = SPOTIFY_URL_PATTERN.matcher(reference.identifier);
        if(!matcher.find()){
            return null;
        }
        try {
            String identifier = matcher.group("identifier");
            switch (matcher.group("type")){
                case "track":
                    return this.getTrack(identifier);

            }
        }
        catch(IOException | ParseException | SpotifyWebApiException e){
            throw new RuntimeException(e);
        }
        return null;
    }

    public SpotifyTrack getTrack(String id) throws IOException, ParseException, SpotifyWebApiException {
        Track spotifyTrack = this.spotify.getTrack(id).build().execute();
        AudioTrackInfo audioTrack = new AudioTrackInfo(spotifyTrack.getName(), spotifyTrack.getArtists()[0].getName(), spotifyTrack.getDurationMs(), spotifyTrack.getId(), false, spotifyTrack.getUri(), spotifyTrack.getAlbum().getImages()[0].getUrl());
        return new SpotifyTrack(audioTrack, spotifyTrack.getExternalIds().getExternalIds().getOrDefault("isrc", null), this);
    }

    @Override
    public boolean isTrackEncodable(AudioTrack track) {
        return true;
    }

    @Override
    public void encodeTrack(AudioTrack track, DataOutput output) throws IOException {

    }

    @Override
    public AudioTrack decodeTrack(AudioTrackInfo trackInfo, DataInput input) throws IOException {
        return null;
    }

    @Override
    public void shutdown() {

    }
}
