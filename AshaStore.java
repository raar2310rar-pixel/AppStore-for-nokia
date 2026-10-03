import javax.microedition.midlet.MIDlet;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.List;
import javax.microedition.lcdui.TextField;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.util.Vector;

public class AshaStore extends MIDlet implements CommandListener {

    private Display display;
    private List main;
    private Form searchForm;
    private TextField searchText;
    private Command searchCommand;
    private Command backCommand;
    private Command exitCommand;

    private List resultsList;
    private Form detailsForm;
    private Command detailsBackCommand;

    private String baseUrl = "http://series40.kiev.ua/";

    public AshaStore() {
        // Конструктор остается пустым для предотвращения сбоев системы при инициализации
    }

    public void startApp() {
        try {
            if (display == null) {
                display = Display.getDisplay(this);
            }

            if (main == null) {
                main = new List("Asha Store", List.IMPLICIT);
                main.append("Последние игры", null);
                main.append("Игры", null);
                main.append("Программы", null);
                main.append("Поиск", null);
                main.append("Обновить", null);

                exitCommand = new Command("Выход", Command.EXIT, 1);
                backCommand = new Command("Назад", Command.BACK, 1);
                detailsBackCommand = new Command("Назад", Command.BACK, 1);
                searchCommand = new Command("Искать", Command.OK, 1);

                main.addCommand(exitCommand);
                main.setCommandListener(this);
            }

            display.setCurrent(main);
        } catch (Throwable t) {
            showFatalError("Ошибка запуска: " + t.getMessage());
        }
    }

    public void pauseApp() {
    }

    public void destroyApp(boolean unconditional) {
    }

    public void commandAction(Command c, Displayable d) {
        try {
            if (c == exitCommand) {
                notifyDestroyed();
                return;
            }

            if (d == main && c == List.SELECT_COMMAND) {
                int n = main.getSelectedIndex();
                if (n == 3) {
                    showSearch();
                } else {
                    loadPage(baseUrl);
                }
                return;
            }

            if (d == searchForm && c == searchCommand) {
                String q = searchText.getString();
                if (q == null || q.trim().length() == 0) {
                    Alert a = new Alert("Поиск", "Введите запрос.", null, AlertType.INFO);
                    a.setTimeout(Alert.FOREVER);
                    display.setCurrent(a, searchForm);
                    return;
                }

                String url = baseUrl + "?s=" + encode(q);
                loadPage(url);
                return;
            }

            if (d == resultsList && c == List.SELECT_COMMAND) {
                int n = resultsList.getSelectedIndex();
                if (n >= 0 && n < resultsList.size()) {
                    String name = resultsList.getString(n);
                    showDetails(name);
                }
                return;
            }

            if (c == backCommand || c == detailsBackCommand) {
                display.setCurrent(main);
            }
        } catch (Throwable t) {
            showFatalError("Ошибка: " + t.getMessage());
        }
    }

    private void showSearch() {
        try {
            searchForm = new Form("Поиск");
            searchText = new TextField("Название:", "", 40, TextField.ANY);
            searchForm.append(searchText);
            searchForm.addCommand(searchCommand);
            searchForm.addCommand(backCommand);
            searchForm.setCommandListener(this);
            display.setCurrent(searchForm);
        } catch (Throwable t) {
            showFatalError("Ошибка поиска: " + t.getMessage());
        }
    }

    private void loadPage(final String url) {
        try {
            final Form wait = new Form("Asha Store");
            wait.append("Подключение...\n\n" + url);
            display.setCurrent(wait);

            new Thread(new Runnable() {
                public void run() {
                    try {
                        String html = downloadText(url);
                        if (html == null || html.length() == 0) {
                            showOffline();
                            return;
                        }
                        showResults(html);
                    } catch (Throwable e) {
                        showOffline();
                    }
                }
            }).start();
        } catch (Throwable t) {
            showOffline();
        }
    }

    private String downloadText(String url) throws Exception {
        HttpConnection con = null;
        InputStream in = null;
        ByteArrayOutputStream out = null;

        try {
            con = (HttpConnection) Connector.open(url, Connector.READ, true);
            con.setRequestMethod(HttpConnection.GET);

            int code = con.getResponseCode();
            if (code != HttpConnection.HTTP_OK) {
                return null;
            }

            in = con.openInputStream();
            out = new ByteArrayOutputStream();

            byte[] buffer = new byte[256];
            int count;

            while ((count = in.read(buffer)) != -1) {
                out.write(buffer, 0, count);
            }

            byte[] data = out.toByteArray();
            try {
                return new String(data, "UTF-8");
            } catch (Exception e) {
                return new String(data);
            }

        } finally {
            if (out != null) {
                try { out.close(); } catch (Exception e) {}
            }
            if (in != null) {
                try { in.close(); } catch (Exception e) {}
            }
            if (con != null) {
                try { con.close(); } catch (Exception e) {}
            }
        }
    }

    private void showResults(String html) {
        try {
            resultsList = new List("Asha Store", List.IMPLICIT);
            Vector names = new Vector();

            int pos = 0;
            while (pos < html.length()) {
                int a = html.indexOf("<a", pos);
                if (a == -1) break;

                int b = html.indexOf(">", a);
                if (b == -1) break;

                int e = html.indexOf("</a>", b);
                if (e == -1) break;

                String text = html.substring(b + 1, e);
                text = stripTags(text);
                text = decode(text);
                text = text.trim();

                if (text.length() > 2 && text.length() < 70) {
                    if (names.indexOf(text) == -1) {
                        names.addElement(text);
                        resultsList.append(text, null);
                    }
                }

                pos = e + 4;
                if (names.size() >= 25) break;
            }

            if (resultsList.size() == 0) {
                resultsList.append("Ничего не найдено", null);
            }

            resultsList.setCommandListener(this);
            resultsList.addCommand(backCommand);

            display.setCurrent(resultsList);
        } catch (Throwable t) {
            showFatalError("Ошибка парсинга: " + t.getMessage());
        }
    }

    private void showDetails(String name) {
        try {
            detailsForm = new Form(name);
            detailsForm.append("Название:\n" + name + "\n\n");
            detailsForm.append("Источник:\nseries40.kiev.ua\n");
            detailsForm.addCommand(detailsBackCommand);
            detailsForm.setCommandListener(this);

            display.setCurrent(detailsForm);
        } catch (Throwable t) {
            showFatalError("Ошибка описания: " + t.getMessage());
        }
    }

    private String stripTags(String s) {
        if (s == null) return "";
        StringBuffer r = new StringBuffer();
        boolean tag = false;

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '<') {
                tag = true;
            } else if (c == '>') {
                tag = false;
            } else if (!tag) {
                r.append(c);
            }
        }
        return r.toString();
    }

    private String decode(String s) {
        if (s == null) return "";
        s = replaceAll(s, "&nbsp;", " ");
        s = replaceAll(s, "&quot;", "\"");
        s = replaceAll(s, "&amp;", "&");
        s = replaceAll(s, "&lt;", "<");
        s = replaceAll(s, "&gt;", ">");
        return s;
    }

    private String replaceAll(String source, String pattern, String replacement) {
        if (source == null) return "";
        StringBuffer sb = new StringBuffer();
        int idx = 0;
        int patLen = pattern.length();
        while (true) {
            int found = source.indexOf(pattern, idx);
            if (found == -1) {
                sb.append(source.substring(idx));
                break;
            }
            sb.append(source.substring(idx, found));
            sb.append(replacement);
            idx = found + patLen;
        }
        return sb.toString();
    }

    private String encode(String s) {
        if (s == null) return "";
        StringBuffer r = new StringBuffer();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')) {
                r.append(c);
            } else if (c == ' ') {
                r.append('+');
            } else {
                r.append('%');
                String h = Integer.toHexString(c);
                if (h.length() < 2) {
                    r.append('0');
                }
                r.append(h);
            }
        }
        return r.toString();
    }

    private void showOffline() {
        try {
            Alert a = new Alert("Ошибка сети", "Нет интернет-соединения.", null, AlertType.ERROR);
            a.setTimeout(Alert.FOREVER);
            display.setCurrent(a, main);
        } catch (Throwable t) {}
    }

    private void showFatalError(String msg) {
        try {
            Alert a = new Alert("Сбой", msg, null, AlertType.ERROR);
            a.setTimeout(Alert.FOREVER);
            if (display != null) {
                display.setCurrent(a);
            }
        } catch (Throwable t) {}
    }
}
