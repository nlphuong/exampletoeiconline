package com.pnguyen.core.DAO;

import com.pnguyen.core.data.DAO.GenericDAO;
import com.pnguyen.core.persistence.entity.UserEntity;

public interface UserDAO extends GenericDAO<Integer, UserEntity> {

    UserEntity findUserByUsernameAndPassword(String name, String password);

}
