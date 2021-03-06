package scw.hibernate;

import org.hibernate.Session;
import org.hibernate.SessionFactory;

import scw.context.annotation.Provider;

@Provider(order = Integer.MIN_VALUE, value = Hibernate.class)
public class DefaultHibernate implements Hibernate {
	private final SessionFactory sessionFactory;

	public DefaultHibernate(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	public SessionFactory getSessionFactory() {
		return sessionFactory;
	}

	public Session getTransactionSession() {
		return HibernateUtils.getTransactionSession(getSessionFactory());
	}
}
