package org.example.javafx_example.server.database;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.util.List;

public class UserRepository {
    
    /**
     * Сохранить или обновить пользователя в базе данных
     */
    public void saveUser(UserEntity user) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.saveOrUpdate(user);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            e.printStackTrace();
        }
    }
    
    /**
     * Получить пользователя по имени
     */
    public UserEntity getUserByUsername(String username) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.get(UserEntity.class, username);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Добавить победу пользователю
     */
    public void incrementUserWins(String username) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            
            UserEntity user = session.get(UserEntity.class, username);
            if (user == null) {
                user = new UserEntity(username);
            }
            
            user.incrementWins();
            session.saveOrUpdate(user);
            
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            e.printStackTrace();
        }
    }
    
    /**
     * Получить список всех пользователей, отсортированный по количеству побед (по убыванию)
     */
    public List<UserEntity> getLeaderboard() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<UserEntity> query = session.createQuery(
                    "FROM UserEntity u ORDER BY u.wins DESC", UserEntity.class);
            return query.list();
        } catch (Exception e) {
            e.printStackTrace();
            return List.of(); // Возвращаем пустой список
        }
    }
} 