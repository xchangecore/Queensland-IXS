package com.saic.uicds.core.dao.hb;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Example;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.saic.uicds.core.dao.GenericDAO;

public abstract class GenericHibernateDAO<T, ID extends Serializable> implements GenericDAO<T, ID> {

    Logger log = LoggerFactory.getLogger(GenericHibernateDAO.class);

    private EntityManager em;

    private final Class<T> persistentClass;

    @SuppressWarnings("unchecked")
    public GenericHibernateDAO() {
        this.persistentClass = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        setEntityManager(HibernateUtil.getCurrentEntityManager());
    }

    @Override
    public boolean exists(ID id) {
        return findById(id) != null;
    }

    @Override
    public List<T> findAll() {
        return findByCriteria();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<T> findByExample(T exampleInstance, String... excludeProperty)
            throws HibernateException {
        Criteria crit = getSession().createCriteria(getPersistentClass());
        Example example = Example.create(exampleInstance);
        for (String exclude : excludeProperty) {
            example.excludeProperty(exclude);
        }
        crit.add(example);
        List<T> lstResult = null;
        try {
            lstResult = crit.list();
        } catch (HibernateException he) {
            log.error(he.getMessage());
        }
        return lstResult;
    }

    @Override
    public T findById(ID id) {
        T entity = em.find(getPersistentClass(), id);
        return entity;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T findById(ID id, boolean lock) {
        T entity;
        if (lock) {
            entity = (T) getSession().load(getPersistentClass(), id, LockMode.UPGRADE);
        } else {
            entity = (T) getSession().load(getPersistentClass(), id);
        }

        return entity;
    }

    public void lock(T entity, LockMode lockMode) {
        getSession().lock(entity, lockMode);
    }

    public void flush() {
        getSession().flush();
    }

    @Override
    public T makePersistent(T entity) {
        getSession().saveOrUpdate(entity);
        return entity;
    }

    @Override
    public void makeTransient(T entity) {
        getSession().delete(entity);
    }

    public void refresh(T entity) {
        getSession().refresh(entity);
    }

    @PersistenceContext
    public void setEntityManager(EntityManager em) {
        this.em = em;
    }

    protected void clear() {
        getSession().clear();
    }

    /**
     * Use this inside subclasses as a convenience method.
     */
    @SuppressWarnings("unchecked")
    protected List<T> findByCriteria(Criterion... criterion) {
        Criteria crit = getSession().createCriteria(getPersistentClass());
        for (Criterion c : criterion) {
            crit.add(c);
        }
        return crit.list();
    }

    protected EntityManager getEntityManager() {
        if (em == null) {
            throw new IllegalStateException("EntityManager has not be set on DAO before usage");
        }
        return em;
    }

    protected Class<T> getPersistentClass() {
        return persistentClass;
    }

    protected Session getSession() {
        if (em == null) {
            throw new IllegalStateException("Session has not been set on DAO before usage");
        }
        Session result = (Session) em.getDelegate();
        return result;
    }

    public boolean isSessionInitialized() {
        if (log.isDebugEnabled()) {
            log.debug("isSessionInitialized=" + getSession().isOpen());
        }
        return getSession().isOpen();
    }
}
