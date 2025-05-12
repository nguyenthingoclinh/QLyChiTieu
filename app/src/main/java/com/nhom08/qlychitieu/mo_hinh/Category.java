package com.nhom08.qlychitieu.mo_hinh;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "categories",
        foreignKeys = @ForeignKey(
                entity = User.class,
                parentColumns = "userId",
                childColumns = "userId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index(value = "userId")}
)
public class Category {
    @PrimaryKey(autoGenerate = true)
    private int categoryId;

    private int userId;
    private String name;
    private String type;  // "Expense" hoặc "Income"
    private String icon;

    @Ignore
    private String displayType;

    @Ignore
    public Category() {}

    public Category(int userId, String name, String type, String icon) {
        this.userId = userId;
        this.name = name;
        this.type = type;
        this.icon = icon;
    }

    public int getCategoryId() { return categoryId; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public String getDisplayType() { return displayType; }
    public void setDisplayType(String displayType) { this.displayType = displayType; }
}