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
import java.util.Map;

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
    public Object[] findByProperty(Map<String, Object> properties, String sortExpression, String sortDirection, Integer offset, Integer limit) {

        List<T> list = new ArrayList<T>();
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction transaction = session.beginTransaction();
        Object totalItem = 0;
        String[] params = new String[properties.size()];
        Object[] values = new Object[properties.size()];
        int i = 0;
        for (Map.Entry item: properties.entrySet()){
            params[i] = (String) item.getKey();
            values[i] = item.getValue();
            i++;
        }
        try{
            StringBuilder sql1 = new StringBuilder("from ");
            sql1.append(this.getPersistenceClassName());
            if (properties.size() >0){
                for (int i1 =0; i1 < params.length; i1++){
                    if(i1 ==0){
                        sql1.append(" where ").append(params[i1]).append(" = :"+params[i1]+" ");
                    }else{
                        sql1.append(" and ").append(params[i1]).append(" = :"+params[i1]+" ");
                    }
                }
            }
            if(sortExpression != null && sortDirection != null){
                sql1.append(" order by ").append(sortExpression);
                sql1.append("  "+(sortDirection.equals(CoreConstant.SORT_ASC)?"ASC":"DESC"));
            }
            Query query1 = session.createQuery(sql1.toString());
            if (properties.size() > 0){
                for (int i2 =0; i2 < params.length; i2++){
                    query1.setParameter(params[i2], values[i2]);
                }
            }
            if(offset != null && offset >=0){
                query1.setFirstResult(offset);
            }
            if (limit != null && limit > 0){
                query1.setMaxResults(limit);
            }
            list = query1.list();

            StringBuilder sql2 = new StringBuilder("select count(*) from ");
            sql2.append(this.getPersistenceClassName());
            if (properties.size() >0){
                for (int k1 =0; k1 < params.length; k1++){
                    if(k1 ==0){
                        sql2.append(" where ").append(params[k1]).append(" = :"+params[k1]+" ");
                    }else{
                        sql2.append(" and ").append(params[k1]).append(" = :"+params[k1]+" ");
                    }
                }
            }
            Query query2 = session.createQuery(sql2.toString());
            if (properties.size() > 0){
                for (int k2 =0; k2 < params.length; k2++){
                    query2.setParameter(params[k2], values[k2]);
                }
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
