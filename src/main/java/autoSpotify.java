import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import java.net.URL;
import java.net.URLConnection;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.wrapper.spotify.model_objects.specification.*;
import com.wrapper.spotify.requests.data.playlists.*;
import com.wrapper.spotify.requests.data.search.simplified.SearchTracksRequest;
import com.google.api.services.youtube.model.Video;
import com.sapher.youtubedl.YoutubeDLException;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.VideoListResponse;
import com.wrapper.spotify.SpotifyApi;
import com.wrapper.spotify.exceptions.SpotifyWebApiException;
import org.apache.hc.core5.http.ParseException;
import java.io.IOException;
import java.util.*;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;

/**API key:  AIzaSyDqaDZ9PI7_ATyditJVAjmfsWFIs1dbs9A*/
/**Spotify User Id:   12136392680
 */
public class autoSpotify {
/**1. Log in youtube
 * 2. Grab our liked vids
 * 3. Make new playlist
 * 4.search for song
 * 5/ add song into new spotify.
 * */
private static final String CLIENT_SECRETS= "client_secrets.json";
    private static final Collection<String> SCOPES =
            Arrays.asList("https://www.googleapis.com/auth/youtube.readonly");
    private static final String APPLICATION_NAME = "youtube";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    public static ArrayList<String> allURIs;
    public static List<String> tracks;

    public static String accessToken = "BQCR16Ow3RIB3rYem_XOIUR70iqRgwj4_lByneuBDC9FLyw9R6W4woHVJKtlYzsEdeXrdmHXQkZiGHHeJw-RTluwAAv9knYHWKpxF1kSu_EGpYC_kJrIFzMZoR88HrSw0_RIUNlnTcSJ-8D7RzeIT_LGdMZwLXg4yoqFl3AhZF3g";
    /** 1. Log in youtube
     * Create an authorized Credential object.
     * @return an authorized Credential object.
     * @throws IOException
     */

    public static Credential authorize(final NetHttpTransport httpTransport) throws IOException {
        // Load client secrets.
        InputStream in = autoSpotify.class.getResourceAsStream(CLIENT_SECRETS);
        GoogleClientSecrets clientSecrets =
                GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));
        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow =
                new GoogleAuthorizationCodeFlow.Builder(httpTransport, JSON_FACTORY, clientSecrets, SCOPES)
                        .build();
        Credential credential =
                new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
        return credential;
    }

    /**
     * Build and return an authorized API client service.
     *
     * @return an authorized API client service
     * @throws GeneralSecurityException, IOException
     */
    public static YouTube getService() throws GeneralSecurityException, IOException {
        final NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        Credential credential = authorize(httpTransport);
        return new YouTube.Builder(httpTransport, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }
    /** 2. Grab playlist liked vids
     *  @throws GeneralSecurityException, IOException, GoogleJsonResponseException
     * */
    public static void get_liked_vids(YouTube youtubeService)
            throws IOException
             {
        // Define and execute the API request
                 long maxRes =50;
        YouTube.Videos.List request = youtubeService.videos()
                .list(Collections.singletonList("snippet,contentDetails,statistics"));
        VideoListResponse response = request.setMyRating("like")
                .setMaxResults(maxRes)
                .execute();
        /**amount of contentDetails Json have, so we can access its data such as snippet and titles*/
        int numOfContentDetails = response.getItems().size();
        String videoURL = "";
        allURIs = new ArrayList<>();
        for(int i = 0 ; i< numOfContentDetails;i++){
            /**accessing individual contents*/
            Video videoInfo = response.getItems().get(i);
            videoURL = "https://www.youtube.com/watch?v="+videoInfo.getId();
            String song_name = fetch_song_name(videoURL);
            if(song_name.length() > 50) {
                continue;
            }
            /**name parsed from HTML is different from Spotify's track names*/
            if(tracks.contains(song_name)){
                continue;
            }
            allURIs.add(search_song(song_name));
        }
    }

    /**make playlist on your spotify account*/
    public static String create_playlist() throws UnirestException, ParseException, SpotifyWebApiException, IOException {
        String userId = "12136392680";
        String name = "youtubeLikedMusic";
        String playListId =check_playList(userId,name);
        /**if playlist exists*/
        if( playListId != null) {
            return playListId;
        }
        playListId="";
        SpotifyApi spotifyApi = new SpotifyApi.Builder()
                .setAccessToken(accessToken)
                .build();
        CreatePlaylistRequest createPlaylistRequest = spotifyApi.createPlaylist(userId,name)
                .build();
        try {
                final Playlist playlist = createPlaylistRequest.execute();
                playListId=playlist.getId();
        } catch (IOException | SpotifyWebApiException | ParseException e) {
            System.out.println("Error: " + e.getMessage());
        }
        return playListId;
    }
    /**check if playlist exists*/
    public static String check_playList(String userId, String playlist_name) throws ParseException, SpotifyWebApiException, IOException {
        SpotifyApi spotifyApi = new SpotifyApi.Builder()
                .setAccessToken(accessToken)
                .build();
        GetListOfUsersPlaylistsRequest getListOfUsersPlaylistsRequest = spotifyApi
                .getListOfUsersPlaylists(userId)
                .build();
        Paging<PlaylistSimplified> playlistSimplifiedPaging = getListOfUsersPlaylistsRequest.execute();
        for(PlaylistSimplified i : playlistSimplifiedPaging.getItems()){
            if(playlist_name.equals(i.getName())) return i.getId();
        }
        return null;
    }

    /**search song on spotify*/
    public static String search_song(String song_name){
        SpotifyApi spotifyApi = new SpotifyApi.Builder()
                .setAccessToken(accessToken)
                .build();
        SearchTracksRequest searchTracksRequest  = spotifyApi.searchTracks(song_name)
                .build();
        String uri ="";
        try {
            final Paging<Track> searchResult = searchTracksRequest.execute();
            uri = searchResult.getItems()[0].getUri();
        } catch (IOException | SpotifyWebApiException | ParseException e) {
            System.out.println("Error: " + e.getMessage());
        }
        return uri;
    }

    /**fetch song name thru HTML data. need to have "song" section in tag on youtube description*/
    public static String fetch_song_name(String videoURL){
        String content = null;
        URLConnection connection = null;
        try {
            /**between
             * "title":{"simpleText":"Song"},"contents":[{"simpleText":"..."}]
             * */
            connection =  new URL(videoURL).openConnection();
            Scanner scanner = new Scanner(connection.getInputStream());
            scanner.useDelimiter("\\Z");
            content = scanner.next();
            scanner.close();
        }catch ( Exception ex ) {
            ex.printStackTrace();
        }
        String start = "\"title\":{\"simpleText\":\"Song\"},\"contents\":[{\"simpleText\":\"";
        String end = "}]";
        int indexFirst = content.indexOf(start);
        int indexSecond = content.indexOf(end,indexFirst);
        String song_name = content.substring(indexFirst+start.length(),indexSecond-1);
        return song_name;
    }
    public static void get_playlist_tracks(String playlistId)
            throws ParseException, SpotifyWebApiException, IOException {
        tracks = new ArrayList<>();
        SpotifyApi spotifyApi = new SpotifyApi.Builder()
                .setAccessToken(accessToken)
                .build();
        GetPlaylistsItemsRequest getPlaylistsItemsRequest = spotifyApi
                .getPlaylistsItems(playlistId)
                .build();
        Paging<PlaylistTrack> playlistTrackPaging = getPlaylistsItemsRequest.execute();
        for(PlaylistTrack t : playlistTrackPaging.getItems()){
            tracks.add(((Track)(t.getTrack())).getName());
        }
    }
    public static void add_songs_to_playlist() throws GeneralSecurityException, IOException, UnirestException, ParseException, SpotifyWebApiException {
        YouTube youtubeService = getService();
        String playlist_id = create_playlist();
        get_playlist_tracks(playlist_id);
        get_liked_vids(youtubeService);
        SpotifyApi spotifyApi = new SpotifyApi.Builder()
                .setAccessToken(accessToken)
                .build();
        String[] collectedURIsArray = new String[allURIs.size()];
        allURIs.toArray(collectedURIsArray);
        AddItemsToPlaylistRequest addItemsToPlaylistRequest  = spotifyApi
                .addItemsToPlaylist(playlist_id,collectedURIsArray)
                .build();
        addItemsToPlaylistRequest.execute();
    }
    /**
     * Call function to create API service object. Define and
     * execute API request. Print API response.
     *
     * @throws GeneralSecurityException, IOException, GoogleJsonResponseException
     */
    public static void main(String[] args)
            throws GeneralSecurityException, IOException, GoogleJsonResponseException, YoutubeDLException, UnirestException, ParseException, SpotifyWebApiException {
        add_songs_to_playlist();
    }


}
