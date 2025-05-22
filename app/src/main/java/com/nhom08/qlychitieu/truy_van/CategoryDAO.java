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
    long insertCategory(Category category);
    @Insert
    void insertCategories(List<Category> categories);
    @Update
    void updateCategory(Category category);
    @Delete
    void deleteCategory(Category category);
    @Query("DELETE FROM categories WHERE userId = :userId")
    void deleteAllByUserId(int userId);
    @Query("SELECT * FROM categories WHERE userId = :userId")
    List<Category> getAllCategories(int userId);
    @Query("SELECT * FROM categories WHERE userId = :userId AND type = :type")
    List<Category> getCategoriesByType(int userId, String type);
    @Query("SELECT * FROM categories WHERE userId = :userId")
    List<Category> getCategoryByUserId(int userId);
}
