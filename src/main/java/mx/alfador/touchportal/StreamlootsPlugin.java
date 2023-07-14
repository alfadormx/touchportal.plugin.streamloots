package mx.alfador.touchportal;

import com.christophecvb.touchportal.annotations.*;
import com.christophecvb.touchportal.helpers.PluginHelper;
import com.christophecvb.touchportal.TouchPortalPlugin;
import com.christophecvb.touchportal.model.*;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mx.alfador.touchportal.models.StreamlootsCardModel;
import mx.alfador.touchportal.models.StreamlootsPurchaseModel;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

@Plugin(version = BuildConfig.VERSION_CODE, colorDark = "#203060", colorLight = "#4070F0", name = "Streamloots plugin - alFadorMX")
public class StreamlootsPlugin extends TouchPortalPlugin implements TouchPortalPlugin.TouchPortalPluginListener {

    private final static Logger LOGGER = Logger.getLogger(TouchPortalPlugin.class.getName());
    private ExecutorService executorService;
    private final Gson gson;

    private enum Categories {
        @Category(name = "Streamloots", imagePath = "images/icon-24.png")
        BaseCategory,
    }

    @Setting(name = "Streamloots ID", defaultValue = "", maxLength = 68.0D)
    private String streamlootsID;

    @State(desc = "Streamloots Chest: Purchased By", defaultValue = "", categoryId = "BaseCategory")
    private String chestPurchasedBy;

    @State(desc = "Streamloots Chest: Gifted By", defaultValue = "", categoryId = "BaseCategory")
    private String chestGiftedBy;

    @State(desc = "Streamloots Chest: Quantity", defaultValue = "", categoryId = "BaseCategory")
    private String chestQuantity;

    @State(desc = "Streamloots Card: Name", defaultValue = "", categoryId = "BaseCategory")
    private String cardName;

    @State(desc = "Streamloots Card: Image", defaultValue = "", categoryId = "BaseCategory")
    private String cardImage;

    @State(desc = "Streamloots Card: Description", defaultValue = "", categoryId = "BaseCategory")
    private String cardDescription;

    @State(desc = "Streamloots Card: Redeemed By", defaultValue = "", categoryId = "BaseCategory")
    private String cardRedeemedBy;

    @State(desc = "Streamloots Card: Alert Message", defaultValue = "", categoryId = "BaseCategory")
    private String cardAlertMessage;

    @State(desc = "Streamloots Card: Field (Message)", defaultValue = "", categoryId = "BaseCategory")
    private String cardFieldMessage;

    public StreamlootsPlugin() {
        super(true);

        this.gson = new Gson();
        this.connectThenPairAndListen(this);
    }

    private void startUpdatingStatesAndChoices() {
        if (this.executorService != null) {
            this.executorService.shutdownNow();
        }

        this.executorService = Executors.newSingleThreadExecutor();

        if (!isNullOrEmpty(this.streamlootsID)) {
            this.executorService.submit(this::backgroundCheck);
        }
    }

    private void backgroundCheck() {
        try {
            URL url = new URL(String.format("https://widgets.streamloots.com/alerts/%1$s/media-stream", this.streamlootsID));

            HttpURLConnection uc = (HttpURLConnection)url.openConnection();
            uc.setRequestMethod("GET");
            uc.connect();

            InputStream inputStream = uc.getInputStream();
            StringBuilder cardDataSB = new StringBuilder();
            byte[] buffer = new byte[100000];

            while (true) {
                int len = inputStream.read(buffer, 0, 100000);
                if (len > 10) {
                    String text = new String(Arrays.copyOfRange(buffer, 0, len), StandardCharsets.UTF_8);
                    if (!isNullOrEmpty(text)) {
                        cardDataSB.append(text);
                        try {
                            String toParse = "{" + cardDataSB.toString() + "}";
                            JsonObject jObject = JsonParser.parseString(toParse).getAsJsonObject();
                            if (jObject != null && jObject.has("data")) {
                                cardDataSB.setLength(0);
                                if (jObject.getAsJsonObject("data").has("data") && jObject.getAsJsonObject("data").getAsJsonObject("data").has("type")) {
                                    String type = jObject.getAsJsonObject("data").getAsJsonObject("data").getAsJsonPrimitive("type").getAsString();
                                    switch (type.toLowerCase(Locale.ROOT)) {
                                        case "purchase":
                                            this.processChestPurchase(jObject);
                                            break;
                                        case "redemption":
                                            this.processCardRedemption(jObject);
                                            break;
                                        default:
                                            StreamlootsPlugin.LOGGER.log(Level.INFO, "Unknown Streamloots packet type: " + type);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            StreamlootsPlugin.LOGGER.log(Level.SEVERE, "Error parsing Streamloots data", e);
                        }
                    }
                }
            }
        } catch(Exception e) {
            StreamlootsPlugin.LOGGER.log(Level.SEVERE, "Error connecting to desired media-stream!!!", e);
        }
    }

    private void processChestPurchase(JsonObject jObject) {
        StreamlootsPurchaseModel purchase = gson.fromJson(jObject.get("data"), StreamlootsPurchaseModel.class);
        if (purchase != null) {
            // clean
            this.chestPurchasedBy = "";
            this.sendStateUpdate(StreamlootsPluginConstants.BaseCategory.States.ChestPurchasedBy.ID, this.chestPurchasedBy, true, true);

            // fill and send
            this.chestQuantity = Integer.toString(purchase.data.getQuantity());
            this.chestGiftedBy = purchase.data.getGiftee();
            this.chestPurchasedBy = purchase.data.getUsername();

            this.sendStateUpdate(StreamlootsPluginConstants.BaseCategory.States.ChestQuantity.ID, this.chestQuantity, true, true);
            this.sendStateUpdate(StreamlootsPluginConstants.BaseCategory.States.ChestGiftedBy.ID, this.chestGiftedBy, true, true);
            this.sendStateUpdate(StreamlootsPluginConstants.BaseCategory.States.ChestPurchasedBy.ID, this.chestPurchasedBy, true, true);

            // report
            // StreamlootsPlugin.LOGGER.log(Level.INFO, "Quantity: " + purchase.data.getQuantity());
            // StreamlootsPlugin.LOGGER.log(Level.INFO, "Gifted by: " + purchase.data.getGiftee());
            // StreamlootsPlugin.LOGGER.log(Level.INFO, "Purchase by: " + purchase.data.getUsername());
        }
    }

    private void processCardRedemption(JsonObject jObject) {
        StreamlootsCardModel card = gson.fromJson(jObject.get("data"), StreamlootsCardModel.class);
        if (card != null) {
            // clean
            this.cardName = "";
            this.sendStateUpdate(StreamlootsPluginConstants.BaseCategory.States.CardName.ID, this.cardName, true, true);

            // fill and send
            this.cardImage = card.imageUrl;
            this.cardRedeemedBy = card.data.getUsername();
            this.cardAlertMessage = card.message;
            this.cardFieldMessage = card.data.getMessage();
            this.cardName = card.data.cardName;

            this.sendStateUpdate(StreamlootsPluginConstants.BaseCategory.States.CardImage.ID, this.cardImage, true, true);
            this.sendStateUpdate(StreamlootsPluginConstants.BaseCategory.States.CardRedeemedBy.ID, this.cardRedeemedBy, true, true);
            this.sendStateUpdate(StreamlootsPluginConstants.BaseCategory.States.CardAlertMessage.ID, this.cardAlertMessage, true, true);
            this.sendStateUpdate(StreamlootsPluginConstants.BaseCategory.States.CardFieldMessage.ID, this.cardFieldMessage, true, true);
            this.sendStateUpdate(StreamlootsPluginConstants.BaseCategory.States.CardName.ID, this.cardName, true, true);

            // report
            // StreamlootsPlugin.LOGGER.log(Level.INFO, "Card Image: " + card.imageUrl);
            // StreamlootsPlugin.LOGGER.log(Level.INFO, "Redeemed By: " + card.data.getUsername());
            // StreamlootsPlugin.LOGGER.log(Level.INFO, "Message: " + card.data.getMessage());
            // StreamlootsPlugin.LOGGER.log(Level.INFO, "Field Message: " + card.data.getMessage());
            // StreamlootsPlugin.LOGGER.log(Level.INFO, "Card Name: " + card.data .cardName);
        }
    }

    private boolean isNullOrEmpty(String s) {
        return s == null || s.length() == 0;
    }

    @Override
    public void onDisconnected(Exception exception) {
        this.executorService.shutdownNow();
        System.exit(0);
    }

    @Override
    public void onReceived(JsonObject jsonMessage) { }

    @Override
    public void onInfo(TPInfoMessage tpInfoMessage) {
        if (!this.isNullOrEmpty(tpInfoMessage.settings.get("Streamloots ID"))) {
            this.startUpdatingStatesAndChoices();
        }
    }

    @Override
    public void onListChanged(TPListChangedMessage tpListChangedMessage) { }

    @Override
    public void onBroadcast(TPBroadcastMessage tpBroadcastMessage) { }

    @Override
    public void onSettings(TPSettingsMessage tpSettingsMessage) {
        if (!this.isNullOrEmpty(tpSettingsMessage.settings.get("Streamloots ID"))) {
            this.startUpdatingStatesAndChoices();
        }
    }

    @Override
    public void onNotificationOptionClicked(TPNotificationOptionClickedMessage tpNotificationOptionClickedMessage) { }

    public static void main(String[] args) {
        if (args != null && args.length == 1 && PluginHelper.COMMAND_START.equals(args[0])) {
            new StreamlootsPlugin();
        }
    }
}