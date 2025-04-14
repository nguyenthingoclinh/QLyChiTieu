package com.nhom08.qlychitieu.tien_ich;
import org.mindrot.jbcrypt.BCrypt;
public class PasswordUtil {

    //Mã hóa
    public static String hashPassword(String password){
        return BCrypt.hashpw(password, BCrypt.gensalt(12)); //12 là độ mạnh của salt
    }

    //Kiểm tra
    public static boolean checkPassword(String password, String hashedPassword){
        return BCrypt.checkpw(password, hashedPassword);
    }
}
