import javax.microedition.midlet.MIDlet;
import javax.microedition.lcdui.*;

public class AshaStore extends MIDlet implements CommandListener {

    private Display display;

    private List mainMenuList;
    private List catalogList;
    private Form searchForm;
    private Form detailForm;
    private Form settingsForm;
    private Form aboutForm;
    private Form debugForm;

    private TextField searchInput;

    private String[] itemUrls;
    private String[] itemNames;
    private int itemCount;

    private String selectedDownloadUrl;

    private String baseUrl;

    private Command exitCmd;
    private Command backCmd;
    private Command executeSearchCmd;
    private Command downloadCmd;
    private Command refreshCmd;

    public AshaStore() {
    }

    protected void startApp() {
        if (display == null) {
            display = Display.getDisplay(this);
        }

        if (mainMenuList == null) {
            baseUrl = "http://series40.kiev.ua/";

            itemUrls = new String[40];
            itemNames = new String[40];

            initCommands();
            initSearchForm();
            initMainMenu();
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
        executeSearchCmd = new Command("Искать", Command.OK, 1);
        downloadCmd = new Command("Скачать", Command.OK, 1);
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

        addCatalogItem("Snake", baseUrl + "snake.jad");
        addCatalogItem("Tetris", baseUrl + "tetris.jad");
        addCatalogItem("Opera Mini", baseUrl + "opera-mini.jad");
        addCatalogItem("UC Browser", baseUrl + "ucbrowser.jad");
        addCatalogItem("Bluetooth Chat", baseUrl + "bluetooth-chat.jad");

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

        detailForm.append(
            new StringItem("Название:", itemNames[index])
        );

        detailForm.append(
            new StringItem(
                "Описание:",
                "Приложение для Nokia Asha."
            )
        );

        detailForm.append(
            new StringItem("JAD:", itemUrls[index])
        );

        selectedDownloadUrl = itemUrls[index];

        detailForm.addCommand(downloadCmd);
        detailForm.addCommand(backCmd);
        detailForm.setCommandListener(this);

        display.setCurrent(detailForm);
    }

    private void initSettings() {
        settingsForm = new Form("Настройки");

        settingsForm.append(
            new StringItem("Сервер:", baseUrl)
        );

        settingsForm.append(
            new StringItem("Версия:", "Asha Store 1.2")
        );

        settingsForm.addCommand(backCmd);
        settingsForm.setCommandListener(this);

        display.setCurrent(settingsForm);
    }

    private void initAbout() {
        aboutForm = new Form("О программе");

        aboutForm.append(
            new StringItem(null, "Asha Store")
        );

        aboutForm.append(
            new StringItem(
                null,
                "Магазин приложений для Nokia Asha."
            )
        );

        aboutForm.append(
            new StringItem(null, "Версия 1.2")
        );

        aboutForm.append(
            new StringItem(null, "Java ME / Asha Platform")
        );

        aboutForm.addCommand(backCmd);
        aboutForm.setCommandListener(this);

        display.setCurrent(aboutForm);
    }

    private void initDebug() {
        debugForm = new Form("Отладка");

        debugForm.append(
            new StringItem(
                null,
                "Asha Store\n\n" +
                "Состояние: работает\n" +
                "Сервер: " + baseUrl + "\n" +
                "Элементов: " + itemCount
            )
        );

        debugForm.addCommand(backCmd);
        debugForm.setCommandListener(this);

        display.setCurrent(debugForm);
    }

    private void performSearch(String query) {
        if (query == null) {
            return;
        }

        query = query.trim().toLowerCase();

        if (query.length() == 0) {
            showInfo(
                "Поиск",
                "Введите поисковый запрос."
            );
            return;
        }

        catalogList = new List(
            "Результаты",
            List.IMPLICIT
        );

        itemCount = 0;

        if (query.indexOf("snake") >= 0) {
            addCatalogItem(
                "Snake",
                baseUrl + "snake.jad"
            );
        }

        if (query.indexOf("tetris") >= 0) {
            addCatalogItem(
                "Tetris",
                baseUrl + "tetris.jad"
            );
        }

        if (query.indexOf("opera") >= 0) {
            addCatalogItem(
                "Opera Mini",
                baseUrl + "opera-mini.jad"
            );
        }

        if (query.indexOf("uc") >= 0 ||
            query.indexOf("browser") >= 0) {

            addCatalogItem(
                "UC Browser",
                baseUrl + "ucbrowser.jad"
            );
        }

        if (query.indexOf("bluetooth") >= 0 ||
            query.indexOf("chat") >= 0) {

            addCatalogItem(
                "Bluetooth Chat",
                baseUrl + "bluetooth-chat.jad"
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

        display.setCurrent(catalogList);
    }

    private void downloadSelected() {
        if (selectedDownloadUrl == null) {
            showInfo(
                "Ошибка",
                "Файл не выбран."
            );
            return;
        }

        showInfo(
            "Загрузка",
            "Пока только адрес:\n\n" +
            selectedDownloadUrl +
            "\n\nЗагрузка JAR будет добавлена позже."
        );
    }

    private void refreshCatalog() {
        initCatalog("Каталог");
    }

    private void showInfo(String title, String message) {
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

        if (command == executeSearchCmd) {
            performSearch(searchInput.getString());
            return;
        }

        if (command == downloadCmd) {
            downloadSelected();
            return;
        }

        if (command == refreshCmd) {
            refreshCatalog();
            return;
        }

        if (displayable == mainMenuList &&
            command == List.SELECT_COMMAND) {

            int selected =
                mainMenuList.getSelectedIndex();

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

            int selected =
                catalogList.getSelectedIndex();

            if (selected >= 0 &&
                selected < itemCount) {

                showDetail(selected);
            }
        }
    }
}
