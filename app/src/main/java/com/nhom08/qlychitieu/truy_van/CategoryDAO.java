package com.nhom08.qlychitieu.truy_van;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;

import com.nhom08.qlychitieu.mo_hinh.Category;

import java.util.List;
@Dao
public interface CategoryDAO {
    @Insert
    void insertCategory(Category category);
    @Update
    void updateCategory(Category category);
    @Delete
    void deleteCategory(Category category);
    @Query("SELECT * FROM categories WHERE userID = :userID")
    List<Category> getAllCategories(int userID);

    @Query("SELECT * FROM categories WHERE userID = :userID AND type = :type")
    List<Category> getCategoriesByType(int userID, String type);
}
