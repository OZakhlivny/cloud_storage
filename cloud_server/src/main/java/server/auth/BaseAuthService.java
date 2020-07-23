package server.auth;


import java.util.Arrays;
import java.util.List;

public class BaseAuthService implements AuthService {

    private static class UserData{
        private String login;
        private String password;
        private String username;

        public UserData(String login, String password, String username) {
            this.login = login;
            this.password = password;
            this.username = username;
        }
    }

    private static final List<UserData> USER_DATA = Arrays.asList(
            new UserData("login1", "pass1", "username1"),
            new UserData("login2", "pass2", "username2"),
            new UserData("login3", "pass3", "username3")
    );

    @Override
    public String getUsernameByLoginAndPassword(String login, String password) {
        for (UserData userDatum : USER_DATA) {
            if (userDatum.login.equals(login) && userDatum.password.equals(password)) {
                return userDatum.username;
            }
        }
        return null;
    }

    @Override
    public void start() {
        System.out.println("Authentication service is running...");
    }

    @Override
    public void stop() {
        System.out.println("Authentication service is stopped.");
    }
}
