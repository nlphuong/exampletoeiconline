package com.pnguyen.core.data.DAOImpl;

import com.pnguyen.core.common.constant.CoreConstant;
import com.pnguyen.core.common.utils.HibernateUtil;
import com.pnguyen.core.data.DAO.GenericDAO;
import org.hibernate.HibernateException;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;

public class GenericDAOImpl<ID extends Serializable, T> implements GenericDAO<ID, T> {

    private Class<T> persistenceClass;

    public GenericDAOImpl() {
        this.persistenceClass = (Class<T>) ((ParameterizedType)getClass().getGenericSuperclass()).getActualTypeArguments()[1];
    }

    public String getPersistenceClassName(){
        return persistenceClass.getSimpleName();
    }



    @Override
    public List<T> findAll() {
        List<T>  list = new ArrayList<T>();
        Transaction transaction = null;
        Session session = HibernateUtil.getSessionFactory().openSession();
        try{
            transaction = session.beginTransaction();
            /*
             * HQL
             * */
            StringBuilder sql = new StringBuilder("from ");
            sql.append(this.getPersistenceClassName());
            Query query = session.createQuery(sql.toString());
            list = query.list();
            transaction.commit();
        }catch (HibernateException e){
            transaction.rollback();
            throw e;
        }finally {
            session.close();
        }

        return list;
    }

    @Override
    public T update(T entity) {
        T result = null;
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction transaction = session.beginTransaction();
        try{
            Object merge = session.merge(entity);
            result = (T) merge;
            transaction.commit();
        }catch (HibernateException e){
            transaction.rollback();
            throw e;
        }finally {
            session.close();
        }
        return result;
    }

    @Override
    public void save(T entity) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction transaction = session.beginTransaction();
        try{
            session.persist(entity);
            transaction.commit();
        }catch (HibernateException e){
            transaction.rollback();
            throw e;
        }finally {
            session.close();
        }
    }

    @Override
    public T findById(ID id) {
        T result = null;
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction transaction = session.beginTransaction();
        try{
            result = (T) session.get(persistenceClass, id);
            if(result == null){
                throw new ObjectNotFoundException(" NOT FOUND "+id, null);
            }
        }catch (HibernateException e){
            transaction.rollback();
            throw e;
        }finally {
            session.close();
        }

        return result;
    }

    @Override
    public Object[] findByProperty(String property, Object value, String sortExpression, String sortDirection) {

        List<T> list = new ArrayList<T>();
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction transaction = session.beginTransaction();
        Object totalItem = 0;
        try{
            StringBuilder sql1 = new StringBuilder("from ");
            sql1.append(this.getPersistenceClassName());

            if(property != null && value != null) {
                sql1.append(" where ").append(property).append(" = :value ");
            }
            if(sortExpression != null && sortDirection != null){
                sql1.append("order by").append(sortExpression);
                sql1.append(" "+(sortDirection.equals(CoreConstant.SORT_ASC)?"ASC":"DESC"));
            }
            Query query1 = session.createQuery(sql1.toString());
            if(value != null){
                query1.setParameter("value", value);
            }
            list = query1.list();

            StringBuilder sql2 = new StringBuilder("select count(*) from ");
            sql2.append(this.getPersistenceClassName());
            if(property != null && value != null) {
                sql2.append(" where ").append(property).append(" = :value");
            }
            Query query2 = session.createQuery(sql2.toString());
            if(value != null){
                query2.setParameter("value", value);
            }
            totalItem = query2.list().get(0);

            transaction.commit();
        }catch(HibernateException e){
            transaction.rollback();
            throw e;
        }finally {
            session.close();
        }

        return new Object[]{totalItem,list};
    }

    @Override
    public Integer delete(List<ID> ids) {
        Integer count = 0;
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction transaction = session.beginTransaction();
        try{
            for (ID item : ids) {
                T t = (T) session.get(persistenceClass, item);
                session.delete(t);
                count++;
            }
            transaction.commit();
        }catch (HibernateException e){
            transaction.rollback();
            throw e;
        }finally {
            session.close();
        }
        return count;
    }
}
