
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
import java.net.URLEncoder;
import org.python.util.PythonInterpreter;
import com.google.api.services.youtube.model.Video;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.sapher.youtubedl.YoutubeDL;
import com.sapher.youtubedl.YoutubeDLException;
import com.sapher.youtubedl.YoutubeDLRequest;
import com.sapher.youtubedl.YoutubeDLResponse;
import com.sapher.youtubedl.mapper.VideoInfo;
import org.json.*;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.VideoListResponse;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**API key:  AIzaSyDqaDZ9PI7_ATyditJVAjmfsWFIs1dbs9A*/
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
    //private static final String api_version = "v3";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    /** 1. Log in youtube
     * Create an authorized Credential object.
     *
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
            throws GeneralSecurityException, IOException, GoogleJsonResponseException,
            YoutubeDLException {
        // Define and execute the API request
        YouTube.Videos.List request = youtubeService.videos()
                .list(Collections.singletonList("snippet,contentDetails,statistics"));
        VideoListResponse response = request.setMyRating("like").execute();
        System.out.println(response);
        System.out.println(response.getItems().get(0).getClass().getName());

        System.out.println(response.getItems().get(4).getSnippet().getTitle());
        /**amount of contentDetails Json have, so we can access its data such as snippet and titles*/
        int numOfContentDetails = response.getItems().size();
        String videoTitle = "";
        String videoURL = "";
        for(int i = 0 ; i< numOfContentDetails;i++){
            /**accessing individual contents*/
            Video videoInfo = response.getItems().get(i);
            /**has video title*/
            videoTitle = videoInfo.getSnippet().getTitle();
            /**has video URL*/
            videoURL = "https://www.youtube.com/watch?v="+videoInfo.getId();
            System.out.println(videoURL);
            /**directory to store youtube dl. Might be useless*/
            String directory = System.getProperty("chmod u+x user.home");
            //YoutubeDLResponse dlResponse = youtube_dl_response(videoURL,directory);
            VideoInfo info = YoutubeDL.getVideoInfo(videoURL);
           // System.out.println("description " +info.description);
            System.out.println("displayID " +info.displayId);
            System.out.println("title " +info.title);
            /**store these info into a txt file and read it in python. then python
             * write song , and artist name using youtube_dl
             * */




            //System.out.println("hello "+dlResponse.getOut());


        }
//        JSONArray arr = (JSONArray) response.get("items");
//        JSONObject contentObj = arr.getJSONObject(0);
//        System.out.println(contentObj.get("caption"));
//
//        System.out.println(arr);

    }

    public static YoutubeDLResponse youtube_dl_response(String url, String dir)
            throws YoutubeDLException {
        YoutubeDLRequest request = new YoutubeDLRequest(url,dir);
        request.setOption("ignore-errors");		// --ignore-errors
        request.setOption("output", "%(id)s");	// --output "%(id)s"
        request.setOption("retries", 10);		// --retries 10
        request.setOption("help");		// --ignore-errors


        return YoutubeDL.execute(request);
    }

    /**
     * Call function to create API service object. Define and
     * execute API request. Print API response.
     *
     * @throws GeneralSecurityException, IOException, GoogleJsonResponseException
     */
    public static void main(String[] args)
            throws GeneralSecurityException, IOException, GoogleJsonResponseException, YoutubeDLException {
        YouTube youtubeService = getService();
        get_liked_vids(youtubeService);

    }


}
