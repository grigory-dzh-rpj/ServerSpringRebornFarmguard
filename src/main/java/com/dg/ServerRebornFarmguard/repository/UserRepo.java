package com.dg.ServerRebornFarmguard.repository;

import com.dg.ServerRebornFarmguard.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserRepo extends JpaRepository<UserEntity, Long> {
    List<UserEntity> findAll();

    @Query("SELECT u FROM UserEntity u WHERE u.position = :position")
    List<UserEntity> findByPos(@Param("position") String position);

   UserEntity findByIduser(Long iduser);

   UserEntity findByName(String name);

   List<UserEntity> findAllByDepartment(String department);

   List<UserEntity> findByPosition(String position);

   UserEntity findByChatId(Long chatId);

   UserEntity findIdUserByName(String name);

   UserEntity findByTel(String tel);

   List<UserEntity> findAllChatIdByTracking(String string);




}
