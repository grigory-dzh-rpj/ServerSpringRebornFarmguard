package com.dg.ServerRebornFarmguard.service;



import com.dg.ServerRebornFarmguard.entity.UserEntity;
import com.dg.ServerRebornFarmguard.repository.UserRepo;
import org.apache.xmlbeans.impl.xb.xsdschema.Attribute;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserService {

    @Autowired
    UserRepo userRepo;

    public  List<UserEntity> findAllUsers(){
        return userRepo.findAll();
    }

    public List<String> findAllUsersName(){
       List<UserEntity> entities = userRepo.findAll();
       List<String> usersNames = new ArrayList<>();
       for(UserEntity entity : entities){
           usersNames.add(entity.getName());
       }
       return usersNames;
    }



    public List<UserEntity> findByPos(String pos) {
        return userRepo.findByPos(pos);
    }

    public List<String> findAllUserName(){
        List<UserEntity> userEntities =  userRepo.findAll();
        List<String> usersName = new ArrayList<>();

        for(UserEntity entity : userEntities){
            usersName.add(entity.getName());
        }

        return usersName;
    }

    public List<String> findAllDepartments(){
        List<UserEntity> userEntities = userRepo.findAll();
        List<String> departments = new ArrayList<>();
        for(UserEntity ent : userEntities){
            departments.add(ent.getDepartment());
        }
        return departments;
    }

    public List<UserEntity> findByDepartment(String department){
        return userRepo.findAllByDepartment(department);
    }

    public UserEntity findByUserId(Long id){
        return userRepo.findByIduser(id);
    }

    public UserEntity createUser(UserEntity user) {
        return userRepo.save(user);
    }

    public String deleteUser(Long id){
       UserEntity entity =  findByUserId(id);
       userRepo.delete(entity);
       return entity.getName();
    }


    public String userTelegramRole(Long chatId){

        UserEntity userEntity = userRepo.findByChatId(chatId);
        if(userEntity == null){
            return "user";
        }
        return userEntity.getRole();

    }

    public UserEntity userByChatId(Long chatId){
        UserEntity userEntity = userRepo.findByChatId(chatId);
        return userEntity;
    }

    public Long chatIdUserByName(String name){
        UserEntity entity = userRepo.findByName(name);
        if (entity.getChatId() != null) {
            return entity.getChatId();
        }else{
            return 1L;
        }
    }

    public Long findUserIdByName(String name){
        UserEntity entity = userRepo.findIdUserByName(name);
        return entity.getIduser();

    }

    public UserEntity findByTel(String tel){return userRepo.findByTel(tel);};

    public UserEntity findByName(String name){
        return userRepo.findByName(name);
    }

   public void save (UserEntity entity){
       userRepo.save(entity);
   }


    public List<Long> findAllChatIdByTracking(){
        List<UserEntity> list = userRepo.findAllChatIdByTracking("yes");
        List<Long> longs = new ArrayList<>();
        for (UserEntity entity : list){
            longs.add(entity.getChatId());
        }
        return longs;
    }



}
