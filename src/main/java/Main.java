import org.sqlite.SQLiteDataSource;
import java.sql.*;
import java.util.Random;
import java.util.Scanner;

class BankSystem {

    public BankSystem(DataBaseCard dbCard) {
        this.dbCard = dbCard;
    }

    public void start() {
        dbCard.start();

        Scanner scanner = new Scanner(System.in);
        boolean flag = true;
        while (flag) {
            printInfo();
            int choice = scanner.nextInt();
            System.out.println();
            switch (choice) {
                case 1 :
                    createAnAccount();
                    break;
                case 2 :
                    boolean res = logIntoAccount();
                    if (!res) {
                        System.out.println("Bye!");
                        flag = false;
                    }
                    break;
                case 0 :
                    System.out.println("Bye!");
                    flag = false;
                    break;
            }
        }
    }

    private void printInfo() {
        System.out.println("1. Create an account");
        System.out.println("2. Log into account");
        System.out.println("0. Exit");
    }

    private void createAnAccount() {
        String number = generateCardNumberLuhn();
        Card card = new Card();
        card.setNumber(number);
        dbCard.addCard(card);

        System.out.println("Your card has been created");
        System.out.println("Your card number:");
        System.out.println(number);
        System.out.println("Your card PIN:");
        System.out.println(card.getPin());
        System.out.println();
    }

    private boolean logIntoAccount() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter your card number:");
        String numberInput = scanner.next();
        System.out.println("Enter your PIN:");
        String pinInput = scanner.next();

        Card cardCheck = dbCard.getCardFromDB(numberInput);
        if (!cardCheck.isValid() || !cardCheck.getPin().equals(pinInput)) {
            System.out.println();
            System.out.println("Wrong card number or PIN!");
            System.out.println();
            return true;
        }

        System.out.println();
        System.out.println("You have successfully logged in!");

        boolean flag = true;
        while(flag) {
            System.out.println();
            System.out.println("1. Balance");
            System.out.println("2. Add income");
            System.out.println("3. Do transfer");
            System.out.println("4. Close account");
            System.out.println("5. Log out");
            System.out.println("0. Exit");

            int choice = scanner.nextInt();
            switch (choice) {
                case 0 :
                    System.out.println();
                    return false;
                case 1 :
                    System.out.println();
                    System.out.println("Balance: " + dbCard.getBalanceFromDB(cardCheck.getNumber()));
                    break;
                case 2 :
                    System.out.println();
                    System.out.println("Enter income:");
                    int income = scanner.nextInt();
                    dbCard.topUpAccount(cardCheck.getNumber(), income);
                    System.out.println("Income was added!");
                    break;
                case 3 :
                    System.out.println();
                    System.out.println("Transfer");
                    System.out.println("Enter card number:");
                    String recipientNumber = scanner.next();
                    if (recipientNumber.equals(cardCheck.getNumber())) {
                        System.out.println("You can't transfer money to the same account!");
                        break;
                    } else if (!isLuhnOk(recipientNumber)) {
                        System.out.println("Probably you made a mistake in the card number. Please try again!");
                        break;
                    } else if (!dbCard.getCardFromDB(recipientNumber).isValid()) {
                        System.out.println("Such a card does not exist.");
                        break;
                    } else {
                        System.out.println("Enter how much money you want to transfer:");
                        int val = scanner.nextInt();
                        if (Integer.valueOf(dbCard.getBalanceFromDB(cardCheck.getNumber())) < val) {
                            System.out.println("Not enough money!");
                            break;
                        }
                        dbCard.doTransfer(cardCheck.getNumber(), recipientNumber, val);
                    }
                    break;
                case 4 :
                    System.out.println();
                    System.out.println("The account has been closed!");
                    dbCard.deleteAcc(cardCheck.getNumber());
                    break;
                case 5 :
                    System.out.println();
                    System.out.println("You have successfully logged out!");
                    System.out.println();
                    flag = false;
                    break;
            }
        }
        return true;
    }

    private boolean isLuhnOk(String recipientNumber) {
        if (recipientNumber.length() < 16) {
            return false;
        }
        int sumLuhn = countLuhnSum(recipientNumber);
        int lastVal = recipientNumber.charAt(recipientNumber.length() - 1) - '0';
        return (sumLuhn + lastVal) % 10 == 0;
    }

    private int countLuhnSum(String numbs) {
        int sumLuhn = 0;

        for (int i = 0; i < 15; i++) {
            int curVal = numbs.charAt(i) - '0';
            if (i % 2 == 0) {
                if (curVal * 2 > 9) {
                    curVal = curVal * 2 - 9;
                } else {
                    curVal = curVal * 2;
                }
            }
            sumLuhn += curVal;
        }

        return sumLuhn;
    }


    private String generateCardNumberLuhn() {
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        sb.append("400000");

        for (int i = 0; i < 9; i++) {
            sb.append(random.nextInt(10));
        }

        int sumLuhn = countLuhnSum(sb.toString());
        int lastNumber = 0;
        if (sumLuhn % 10 > 0) {
            lastNumber = 10 - sumLuhn % 10;
        }
        sb.append(lastNumber);

        return sb.toString();
    }

    private DataBaseCard dbCard;
}

class Card {

    public Card() {
        generatePin();
        this.balance = 0;
        this.isValid = true;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }

    public int getBalance() {
        return balance;
    }

    public void setBalance(int balance) {
        this.balance = balance;
    }

    public boolean isValid() {
        return isValid;
    }

    public void setValid(boolean valid) {
        isValid = valid;
    }

    private void generatePin() {
        Random rnd = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            sb.append(rnd.nextInt(10));
        }
        pin = sb.toString();
    }

    private String number = "";
    private String pin;
    private int balance;
    private boolean isValid;
}

class DataBaseCard {
    public DataBaseCard(String url) {
        this.url = url;
    }

    public void start() {
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl(url);

        try (Connection con = dataSource.getConnection()) {
            try (Statement statement = con.createStatement()) {
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS card(" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                        "number TEXT," +
                        "pin TEXT," +
                        "balance INTEGER DEFAULT 0)");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void addCard(Card card) {
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl(url);

        try (Connection con = dataSource.getConnection()) {
            try (Statement statement = con.createStatement()) {
                statement.executeUpdate("INSERT INTO card VALUES " +
                        "(NULL, '" + card.getNumber() + "', '" + card.getPin() + "', " +
                        card.getBalance() + ")");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public String getBalanceFromDB(String cardNumber) {
        String balance = "";

        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl(url);

        try (Connection con = dataSource.getConnection()) {
            try (Statement statement = con.createStatement()) {
                try (ResultSet number = statement.executeQuery("SELECT balance FROM card " +
                        "WHERE number = '" + cardNumber + "'")) {
                    balance = number.getString("balance");
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return balance;
    }

    public Card getCardFromDB(String cardNumber) {
        Card cardResult = new Card();
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl(url);

        try (Connection con = dataSource.getConnection()) {
            try (Statement statement = con.createStatement()) {
                try (ResultSet cards = statement.executeQuery("SELECT * FROM card " +
                        "WHERE number = '" + cardNumber + "'")) {
                    if (cards.next()) {
                        cardResult.setNumber(cards.getString("number"));
                        cardResult.setPin(cards.getString("pin"));
                        cardResult.setBalance(cards.getInt("balance"));
                    } else {
                        cardResult.setNumber("-1");
                        cardResult.setValid(false);
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return cardResult;
    }

    public void topUpAccount(String number, int income) {
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl(url);

        try (Connection con = dataSource.getConnection()) {
            String updateBalance = "UPDATE card SET balance = balance + ? WHERE number = ?";
            try (PreparedStatement preparedStatement = con.prepareStatement(updateBalance)) {
                preparedStatement.setInt(1, income);
                preparedStatement.setString(2, number);
                preparedStatement.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void doTransfer(String number, String recipientNumber, int val) {
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl(url);

        try (Connection con = dataSource.getConnection()) {
            String updateBalance1 = "UPDATE card SET balance = balance - ? WHERE number = ?";
            try (PreparedStatement preparedStatement = con.prepareStatement(updateBalance1)) {
                preparedStatement.setInt(1, val);
                preparedStatement.setString(2, number);
                preparedStatement.executeUpdate();
            }
            String updateBalance2 = "UPDATE card SET balance = balance + ? WHERE number = ?";
            try (PreparedStatement preparedStatement = con.prepareStatement(updateBalance2)) {
                preparedStatement.setInt(1, val);
                preparedStatement.setString(2, recipientNumber);
                preparedStatement.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteAcc(String number) {
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl(url);

        try (Connection con = dataSource.getConnection()) {
            try (Statement statement = con.createStatement()) {
                statement.executeUpdate("DELETE FROM card WHERE number = '" + number + "'");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private String url;
}

public class Main {
    public static void main(String[] args) {
        String url = "jdbc:sqlite:src/card.db";

        DataBaseCard dbCard = new DataBaseCard(url);
        BankSystem bankSystem = new BankSystem(dbCard);
        bankSystem.start();
    }
}
