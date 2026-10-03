import javax.microedition.midlet.MIDlet;
import javax.microedition.lcdui.*;
import javax.microedition.io.*;
import java.io.*;

public class AshaStore extends MIDlet implements CommandListener, Runnable {

    private Display display;

    private List mainMenuList;
    private List catalogList;
    private Form detailForm;
    private Form aboutForm;

    private String[] itemUrls;
    private String[] itemNames;
    private int itemCount;

    private String selectedDownloadUrl;
    private String targetUrl;

    private Command exitCmd;
    private Command backCmd;
    private Command downloadCmd;
    private Command refreshCmd;

    public AshaStore() {
    }

    protected void startApp() {
        if (display == null) {
            display = Display.getDisplay(this);
        }

        if (mainMenuList == null) {
            targetUrl = "http://series40.kiev.ua/";

            itemUrls = new String[50];
            itemNames = new String[50];

            initCommands();
            initMainMenu();
        }

        if (display != null) {
            display.setCurrent(mainMenuList);
        }
    }

    protected void pauseApp() {
    }

    protected void destroyApp(boolean unconditional) {
    }

    private void initCommands() {
        exitCmd = new Command("Выход", Command.EXIT, 1);
        backCmd = new Command("Назад", Command.BACK, 1);
        downloadCmd = new Command("Скачать", Command.OK, 1);
        refreshCmd = new Command("Обновить", Command.SCREEN, 2);
    }

    private void initMainMenu() {
        mainMenuList = new List("Asha Store", List.IMPLICIT);

        mainMenuList.append("Каталог", null);
        mainMenuList.append("О программе", null);

        mainMenuList.addCommand(exitCmd);
        mainMenuList.setCommandListener(this);
    }

    private void startCatalogLoading() {
        Alert loadingAlert = new Alert("Загрузка", "Подключение к серверу...", null, AlertType.INFO);
        loadingAlert.setTimeout(Alert.FOREVER);
        display.setCurrent(loadingAlert);

        Thread thread = new Thread(this);
        thread.start();
    }

    public void run() {
        HttpConnection conn = null;
        InputStream is = null;

        try {
            conn = (HttpConnection) Connector.open(targetUrl, Connector.READ, true);
            conn.setRequestMethod(HttpConnection.GET);

            int responseCode = conn.getResponseCode();

            if (responseCode == HttpConnection.HTTP_OK) {
                is = conn.openInputStream();

                StringBuffer htmlBuffer = new StringBuffer();
                int ch;
                int bytesRead = 0;

                // Читаем не более 20 КБ для защиты Heap на Asha 501
                while ((ch = is.read()) != -1 && bytesRead < 20000) {
                    htmlBuffer.append((char) ch);
                    bytesRead++;
                }

                final String html = htmlBuffer.toString();
                htmlBuffer = null;
                System.gc();

                display.callSerially(new Runnable() {
                    public void run() {
                        parseHtmlAndBuildCatalog(html);
                    }
                });

            } else {
                final int code = responseCode;
                display.callSerially(new Runnable() {
                    public void run() {
                        showInfo("Ошибка", "Код ответа сервера: " + code);
                    }
                });
            }
        } catch (Throwable t) {
            display.callSerially(new Runnable() {
                public void run() {
                    showInfo("Ошибка сети", "Не удалось загрузить данные.");
                }
            });
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception e) {}
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (Exception e) {}
            }
        }
    }

    private void parseHtmlAndBuildCatalog(String html) {
        catalogList = new List("Каталог", List.IMPLICIT);
        itemCount = 0;

        if (html != null) {
            String lowerHtml = html.toLowerCase();
            int cursor = 0;

            while (itemCount < itemNames.length) {
                int hrefIndex = lowerHtml.indexOf("href=", cursor);
                if (hrefIndex == -1) {
                    break;
                }

                int quoteStart = lowerHtml.indexOf("\"", hrefIndex);
                if (quoteStart == -1 || quoteStart > hrefIndex + 10) {
                    cursor = hrefIndex + 5;
                    continue;
                }

                int quoteEnd = lowerHtml.indexOf("\"", quoteStart + 1);
                if (quoteEnd == -1) {
                    cursor = hrefIndex + 5;
                    continue;
                }

                String link = html.substring(quoteStart + 1, quoteEnd);

                int tagClose = lowerHtml.indexOf(">", quoteEnd);
                int aClose = lowerHtml.indexOf("</a>", tagClose);

                String title = "";
                if (tagClose != -1 && aClose != -1 && aClose > tagClose) {
                    title = html.substring(tagClose + 1, aClose).trim();
                }

                if (link.indexOf(".jad") != -1 || link.indexOf(".jar") != -1) {
                    if (!link.startsWith("http://") && !link.startsWith("https://")) {
                        if (link.startsWith("/")) {
                            link = "http://series40.kiev.ua" + link;
                        } else {
                            link = "http://series40.kiev.ua/" + link;
                        }
                    }

                    if (title.length() == 0) {
                        title = "Файл #" + (itemCount + 1);
                    }

                    itemNames[itemCount] = title;
                    itemUrls[itemCount] = link;
                    catalogList.append(title, null);
                    itemCount++;
                }

                cursor = quoteEnd + 1;
            }
        }

        if (itemCount == 0) {
            catalogList.append("Файлы не найдены", null);
        }

        catalogList.addCommand(backCmd);
        catalogList.addCommand(refreshCmd);
        catalogList.setCommandListener(this);

        display.setCurrent(catalogList);
    }

    private void showDetail(int index) {
        if (index < 0 || index >= itemCount) {
            return;
        }

        detailForm = new Form("Приложение");

        detailForm.append(new StringItem("Название:", itemNames[index]));
        detailForm.append(new StringItem("URL:", itemUrls[index]));

        selectedDownloadUrl = itemUrls[index];

        detailForm.addCommand(downloadCmd);
        detailForm.addCommand(backCmd);
        detailForm.setCommandListener(this);

        display.setCurrent(detailForm);
    }

    private void downloadSelected() {
        if (selectedDownloadUrl == null) {
            return;
        }

        try {
            boolean isClosed = platformRequest(selectedDownloadUrl);
            if (isClosed) {
                notifyDestroyed();
            }
        } catch (Exception e) {
            showInfo("Ошибка", "Не удалось запустить браузер.");
        }
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(title, message, null, AlertType.INFO);
        alert.setTimeout(Alert.FOREVER);
        display.setCurrent(alert);
    }

    public void commandAction(Command command, Displayable displayable) {
        if (command == exitCmd) {
            notifyDestroyed();
            return;
        }

        if (command == backCmd) {
            display.setCurrent(mainMenuList);
            return;
        }

        if (command == downloadCmd) {
            downloadSelected();
            return;
        }

        if (command == refreshCmd) {
            startCatalogLoading();
            return;
        }

        if (displayable == mainMenuList && command == List.SELECT_COMMAND) {
            int selected = mainMenuList.getSelectedIndex();
            if (selected == 0) {
                startCatalogLoading();
            } else if (selected == 1) {
                showAbout();
            }
            return;
        }

        if (displayable == catalogList && command == List.SELECT_COMMAND) {
            int selected = catalogList.getSelectedIndex();
            if (selected >= 0 && selected < itemCount) {
                showDetail(selected);
            }
        }
    }

    private void showAbout() {
        aboutForm = new Form("О программе");
        aboutForm.append(new StringItem(null, "Asha Store 1.2\nКлиент каталога"));
        aboutForm.addCommand(backCmd);
        aboutForm.setCommandListener(this);
        display.setCurrent(aboutForm);
    }
}
