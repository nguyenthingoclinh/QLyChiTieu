package com.nhom08.qlychitieu.mo_hinh;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ForeignKey;

@Entity(tableName = "categories",
foreignKeys = @ForeignKey(entity = User.class, parentColumns = "userID", childColumns = "userID", onDelete = ForeignKey.CASCADE))
public class Category {
    @PrimaryKey(autoGenerate = true)
    private int categoryID;
    private int userID;
    private String name;
    private String type;//"Income" hoặc "Expense"
    private String icon;

    public Category(int UserID, String name, String type, String icon) {
        this.userID = UserID;
        this.name = name;
        this.type = type;
        this.icon = icon;
    }

    public int getCategoryID() {
        return categoryID;
    }

    public int getUserID() {
        return userID;
    }

    public void setUserID(int userID) {
        this.userID = userID;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public void setCategoryID(int categoryID) {
        this.categoryID = categoryID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
