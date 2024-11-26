package com.yd.dao;

import com.yd.model.Reel;
import com.yd.util.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ReelsDAO {

    // 데이터베이스 연결 메서드 (DatabaseConnection 클래스가 존재한다고 가정)
    private Connection getConnection() throws SQLException {
        return DatabaseConnection.getConnection();
    }

    // 모든 Reels 가져오기 (페이징 지원)
    public List<Reel> getAllReels(int offset, int limit) {
        List<Reel> reels = new ArrayList<>();
        String sql = "SELECT * FROM REELS ORDER BY CREATED_AT DESC LIMIT ? OFFSET ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            stmt.setInt(2, offset);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Reel reel = new Reel();
                reel.setReelId(rs.getInt("REEL_ID"));
                reel.setVideoUrl(rs.getString("VIDEO_URL"));
                reel.setDescription(rs.getString("DESCRIPTION"));
                reel.setUploaderId(rs.getString("UPLOADER_ID"));
                reel.setCreatedAt(rs.getTimestamp("CREATED_AT").toLocalDateTime());
                reel.setNumOfLikes(rs.getInt("NUM_OF_LIKES"));
                reel.setNumOfComments(rs.getInt("NUM_OF_COMMENTS"));
                reels.add(reel);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return reels;
    }

    // Reel 추가하기
    public boolean addReel(Reel reel) {
        String sql = "INSERT INTO REELS (VIDEO_URL, DESCRIPTION, UPLOADER_ID, CREATED_AT, NUM_OF_LIKES, NUM_OF_COMMENTS) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, reel.getVideoUrl());
            stmt.setString(2, reel.getDescription());
            stmt.setString(3, reel.getUploaderId());
            stmt.setTimestamp(4, Timestamp.valueOf(reel.getCreatedAt()));
            stmt.setInt(5, reel.getNumOfLikes());
            stmt.setInt(6, reel.getNumOfComments());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                return false;
            }

            // 생성된 REEL_ID 가져오기
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    reel.setReelId(generatedKeys.getInt(1));
                } else {
                    throw new SQLException("Reel 추가 시 REEL_ID를 가져올 수 없습니다.");
                }
            }

            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Reel 업데이트하기
    public boolean updateReel(Reel reel) {
        String sql = "UPDATE REELS SET VIDEO_URL = ?, DESCRIPTION = ?, UPLOADER_ID = ?, NUM_OF_LIKES = ?, NUM_OF_COMMENTS = ? WHERE REEL_ID = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, reel.getVideoUrl());
            stmt.setString(2, reel.getDescription());
            stmt.setString(3, reel.getUploaderId());
            stmt.setInt(4, reel.getNumOfLikes());
            stmt.setInt(5, reel.getNumOfComments());
            stmt.setInt(6, reel.getReelId());

            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Reel 삭제하기
    public boolean deleteReel(int reelId) {
        String sql = "DELETE FROM REELS WHERE REEL_ID = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, reelId);
            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Reel 좋아요 수 증가
    public boolean incrementLikes(int reelId) {
        String sql = "UPDATE REELS SET NUM_OF_LIKES = NUM_OF_LIKES + 1 WHERE REEL_ID = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, reelId);
            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Reel 좋아요 수 감소
    public boolean decrementLikes(int reelId) {
        String sql = "UPDATE REELS SET NUM_OF_LIKES = NUM_OF_LIKES - 1 WHERE REEL_ID = ? AND NUM_OF_LIKES > 0";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, reelId);
            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Reel 댓글 수 증가
    public boolean incrementComments(int reelId) {
        String sql = "UPDATE REELS SET NUM_OF_COMMENTS = NUM_OF_COMMENTS + 1 WHERE REEL_ID = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, reelId);
            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Reel 댓글 수 감소
    public boolean decrementComments(int reelId) {
        String sql = "UPDATE REELS SET NUM_OF_COMMENTS = NUM_OF_COMMENTS - 1 WHERE REEL_ID = ? AND NUM_OF_COMMENTS > 0";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, reelId);
            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
