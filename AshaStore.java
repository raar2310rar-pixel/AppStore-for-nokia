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

                isInitialized = true;

            } catch (Exception e) {
                showError("Ошибка запуска", e.toString());
            }
        }

        display.setCurrent(mainMenuList);
    }

    protected void pauseApp() {
    }

    protected void destroyApp(boolean unconditional) {
    }

    private void initCommands() {
        exitCmd = new Command("Выход", Command.EXIT, 1);
        backCmd = new Command("Назад", Command.BACK, 1);
        searchCmd = new Command("Поиск", Command.SCREEN, 1);
        executeSearchCmd = new Command("Искать", Command.OK, 1);
        downloadCmd = new Command("Скачать", Command.OK, 1);
        settingsCmd = new Command("Настройки", Command.SCREEN, 2);
        aboutCmd = new Command("О программе", Command.SCREEN, 3);
        debugCmd = new Command("Отладка", Command.SCREEN, 4);
        refreshCmd = new Command("Обновить", Command.SCREEN, 2);
    }

    private void initMainMenu() {
        mainMenuList = new List("Asha Store", List.IMPLICIT);

        mainMenuList.append("Каталог", null);
        mainMenuList.append("Поиск", null);
        mainMenuList.append("Настройки", null);
        mainMenuList.append("О программе", null);
        mainMenuList.append("Отладка", null);

        mainMenuList.addCommand(exitCmd);
        mainMenuList.setCommandListener(this);
    }

    private void initSearchForm() {
        searchForm = new Form("Поиск");

        searchInput = new TextField(
            "Введите запрос:",
            "",
            50,
            TextField.ANY
        );

        searchForm.append(searchInput);
        searchForm.addCommand(executeSearchCmd);
        searchForm.addCommand(backCmd);
        searchForm.setCommandListener(this);
    }

    private void initCatalog(String title) {
        catalogList = new List(title, List.IMPLICIT);

        itemCount = 0;

        addCatalogItem(
            "Snake",
            baseUrl + "snake.jad"
        );

        addCatalogItem(
            "Tetris",
            baseUrl + "tetris.jad"
        );

        addCatalogItem(
            "Opera Mini",
            baseUrl + "opera-mini.jad"
        );

        addCatalogItem(
            "UC Browser",
            baseUrl + "ucbrowser.jad"
        );

        addCatalogItem(
            "Bluetooth Chat",
            baseUrl + "bluetooth-chat.jad"
        );

        catalogList.addCommand(backCmd);
        catalogList.addCommand(refreshCmd);
        catalogList.setCommandListener(this);

        display.setCurrent(catalogList);
    }

    private void addCatalogItem(String name, String url) {
        if (itemCount >= itemNames.length) {
            return;
        }

        itemNames[itemCount] = name;
        itemUrls[itemCount] = url;

        catalogList.append(name, null);

        itemCount++;
    }

    private void showDetail(int index) {
        if (index < 0 || index >= itemCount) {
            return;
        }

        detailForm = new Form("Приложение");

        appTitleLabel = new StringItem(
            "Название:",
            itemNames[index]
        );

        appDescLabel = new StringItem(
            "Описание:",
            "Приложение для Nokia Asha."
        );

        appUrlLabel = new StringItem(
            "JAD:",
            itemUrls[index]
        );

        detailForm.append(appTitleLabel);
        detailForm.append(appDescLabel);
        detailForm.append(appUrlLabel);

        selectedDownloadUrl = itemUrls[index];

        detailForm.addCommand(downloadCmd);
        detailForm.addCommand(backCmd);
        detailForm.setCommandListener(this);

        display.setCurrent(detailForm);
    }

    private void initSettings() {
        settingsForm = new Form("Настройки");

        StringItem server = new StringItem(
            "Сервер:",
            baseUrl
        );

        StringItem version = new StringItem(
            "Версия:",
            "Asha Store 1.0"
        );

        settingsForm.append(server);
        settingsForm.append(version);

        settingsForm.addCommand(backCmd);
        settingsForm.setCommandListener(this);

        display.setCurrent(settingsForm);
    }

    private void initAbout() {
        aboutForm = new Form("О программе");

        StringItem title = new StringItem(
            null,
            "Asha Store"
        );

        StringItem description = new StringItem(
            null,
            "Магазин приложений для Nokia Asha."
        );

        StringItem version = new StringItem(
            null,
            "Версия 1.0"
        );

        StringItem copyright = new StringItem(
            null,
            "Java ME / Series 40"
        );

        aboutForm.append(title);
        aboutForm.append(description);
        aboutForm.append(version);
        aboutForm.append(copyright);

        aboutForm.addCommand(backCmd);
        aboutForm.setCommandListener(this);

        display.setCurrent(aboutForm);
    }

    private void initDebug() {
        debugForm = new Form("Отладка");

        debugText = new StringItem(
            null,
            "Asha Store\n\n" +
            "Состояние: работает\n" +
            "Сервер: " + baseUrl + "\n" +
            "Элементов: " + itemCount
        );

        debugForm.append(debugText);

        debugForm.addCommand(backCmd);
        debugForm.setCommandListener(this);

        display.setCurrent(debugForm);
    }

    private void performSearch(String query) {
        if (query == null) {
            return;
        }

        query = query.trim();

        if (query.length() == 0) {
            showError(
                "Поиск",
                "Введите поисковый запрос."
            );
            return;
        }

        catalogList = new List(
            "Результаты: " + query,
            List.IMPLICIT
        );

        itemCount = 0;

        String lower = query.toLowerCase();

        if ("snake".indexOf(lower) >= 0 ||
            lower.indexOf("snake") >= 0) {

            addCatalogItem(
                "Snake",
                baseUrl + "snake.jad"
            );
        }

        if ("tetris".indexOf(lower) >= 0 ||
            lower.indexOf("tetris") >= 0) {

            addCatalogItem(
                "Tetris",
                baseUrl + "tetris.jad"
            );
        }

        if (lower.indexOf("opera") >= 0) {
            addCatalogItem(
                "Opera Mini",
                baseUrl + "opera-mini.jad"
            );
        }

        if (lower.indexOf("browser") >= 0 ||
            lower.indexOf("uc") >= 0) {

            addCatalogItem(
                "UC Browser",
                baseUrl + "ucbrowser.jad"
            );
        }

        if (itemCount == 0) {
            catalogList.append(
                "Ничего не найдено",
                null
            );
        }

        catalogList.addCommand(backCmd);
        catalogList.setCommandListener(this);

        currentIsSearch = true;

        display.setCurrent(catalogList);
    }

    private void downloadSelected() {
        if (selectedDownloadUrl == null) {
            showError(
                "Ошибка",
                "Файл не выбран."
            );
            return;
        }

        showError(
            "Загрузка",
            "URL:\n" + selectedDownloadUrl +
            "\n\nВ этой версии загрузка JAR/JAD " +
            "не выполняется автоматически."
        );
    }

    private void refreshCatalog() {
        initCatalog("Каталог");
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(
            title,
            message,
            null,
            AlertType.INFO
        );

        alert.setTimeout(Alert.FOREVER);

        display.setCurrent(alert);
    }

    public void commandAction(
        Command command,
        Displayable displayable
    ) {

        if (command == exitCmd) {
            notifyDestroyed();
            return;
        }

        if (command == backCmd) {
            display.setCurrent(mainMenuList);
            return;
        }

        if (command == searchCmd) {
            display.setCurrent(searchForm);
            return;
        }

        if (command == executeSearchCmd) {
            performSearch(searchInput.getString());
            return;
        }

        if (command == settingsCmd) {
            initSettings();
            return;
        }

        if (command == aboutCmd) {
            initAbout();
            return;
        }

        if (command == debugCmd) {
            initDebug();
            return;
        }

        if (command == refreshCmd) {
            refreshCatalog();
            return;
        }

        if (command == downloadCmd) {
            downloadSelected();
            return;
        }

        if (displayable == mainMenuList &&
            command == List.SELECT_COMMAND) {

            int selected = mainMenuList.getSelectedIndex();

            switch (selected) {
                case 0:
                    initCatalog("Каталог");
                    break;

                case 1:
                    display.setCurrent(searchForm);
                    break;

                case 2:
                    initSettings();
                    break;

                case 3:
                    initAbout();
                    break;

                case 4:
                    initDebug();
                    break;
            }

            return;
        }

        if (displayable == catalogList &&
            command == List.SELECT_COMMAND) {

            int selected = catalogList.getSelectedIndex();

            if (selected >= 0 &&
                selected < itemCount) {

                showDetail(selected);
            }

            return;
        }
    }

    public void run() {
        // Зарезервировано для фоновых операций.
    }
}
