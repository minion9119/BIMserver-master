package org.bimserver.changes;

import java.io.IOException;
import java.util.Collections;

import org.bimserver.BimserverDatabaseException;
import org.bimserver.database.BimserverLockConflictException;
import org.bimserver.database.queries.QueryObjectProvider;
import org.bimserver.database.queries.om.Query;
import org.bimserver.database.queries.om.QueryException;
import org.bimserver.database.queries.om.QueryPart;
import org.bimserver.emf.PackageMetaData;
import org.bimserver.shared.HashMapVirtualObject;
import org.bimserver.shared.exceptions.UserException;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;

public class AddAttributeChange implements Change {

	private final long oid;
	private final String attributeName;
	private final Object value;

	public AddAttributeChange(long oid, String attributeName, Object value) {
		this.oid = oid;
		this.attributeName = attributeName;
		this.value = value;
	}

	@Override
	public void execute(Transaction transaction) throws UserException, BimserverLockConflictException, BimserverDatabaseException, IOException, QueryException {
		PackageMetaData packageMetaData = transaction.getDatabaseSession().getMetaDataManager().getPackageMetaData(transaction.getProject().getSchema());

		HashMapVirtualObject object = transaction.get(oid);
		if (object == null) {
			Query query = new Query(packageMetaData);
			QueryPart queryPart = query.createQueryPart();
			queryPart.addOid(oid);

			QueryObjectProvider queryObjectProvider = new QueryObjectProvider(transaction.getDatabaseSession(), transaction.getBimServer(), query, Collections.singleton(transaction.getPreviousRevision().getOid()), packageMetaData);
			object = queryObjectProvider.next();
			transaction.updated(object);
		}
		
		EClass eClass = transaction.getDatabaseSession().getEClassForOid(oid);
		if (object == null) {
			throw new UserException("No object of type \"" + eClass.getName() + "\" with oid " + oid + " found in project with pid " + transaction.getProject().getId());
		}
		EAttribute eAttribute = packageMetaData.getEAttribute(eClass.getName(), attributeName);
		if (eAttribute == null) {
			throw new UserException("No attribute with the name \"" + attributeName + "\" found in class \"" + eClass.getName() + "\"");
		}
		if (!eAttribute.isMany()) {
			throw new UserException("Attribute is not of type 'many'");
		}
		
		object.addListItem(eAttribute, value);
	}
}