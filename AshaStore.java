import javax.microedition.midlet.*;
import javax.microedition.lcdui.*;
import javax.microedition.io.*;
import java.io.*;

public class AshaStore extends MIDlet implements CommandListener, Runnable {

    private Display display;

    private List mainMenuList;
    private List catalogList;
    private Form searchForm;
    private Form detailForm;
    private Form settingsForm;
    private Form aboutForm;
    private Form debugForm;

    private TextField searchInput;
    private StringItem statusLabel;
    private StringItem appTitleLabel;
    private StringItem appDescLabel;
    private StringItem appUrlLabel;
    private StringItem debugText;

    private Command exitCmd;
    private Command backCmd;
    private Command searchCmd;
    private Command executeSearchCmd;
    private Command downloadCmd;
    private Command settingsCmd;
    private Command aboutCmd;
    private Command debugCmd;
    private Command refreshCmd;

    private String baseUrl;
    private String currentCatalogUrl;
    private boolean currentIsSearch;

    private String selectedDownloadUrl;

    private String[] itemUrls;
    private String[] itemNames;
    private int itemCount;

    private boolean isInitialized = false;

    public AshaStore() {
    }

    protected void startApp() {
        if (display == null) {
            display = Display.getDisplay(this);
        }

        if (!isInitialized) {
            try {
                baseUrl = "http://series40.kiev.ua/";

                itemUrls = new String[40];
                itemNames = new String[40];
                itemCount = 0;

                currentCatalogUrl = null;
                currentIsSearch = false;
                selectedDownloadUrl = null;

                initCommands();
                initMainMenu();
                initSearchForm();
