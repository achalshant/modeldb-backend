package com.mitdbg.modeldb.databaseServices;

import static com.mongodb.client.model.Filters.eq;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.bson.Document;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageOrBuilder;
import com.mitdbg.modeldb.ModelDBConstants;
import com.mitdbg.modeldb.ModelDBUtils;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;

public class MongoService implements DocumentService {

  MongoDatabase database = null;
  String TAG = this.getClass().getName();
  private String collectionName = null;

  public MongoService(MongoDatabase database) {
    this.database = database;
  }

  /*
   * Check availability of given collection name in database, if is not exist then create collection
   * with given collection. This method called from each entity service Impl constructor.
   *
   * @param String collection
   */
  @Override
  public void checkCollectionAvailability(String collection) {
    this.collectionName = collection;
    Boolean availabilityStatus = false;
    MongoIterable<String> collectionsNameList = this.database.listCollectionNames();

    if (collectionsNameList != null) {
      Iterator<String> iterator = collectionsNameList.iterator();

      while (iterator.hasNext()) {
        String collectionName = iterator.next();
        if (collectionName.equals(collection)) {
          availabilityStatus = true;
          break;
        }
      }
    }

    if (!availabilityStatus || collectionsNameList == null) {
      this.database.createCollection(collection);
    }
  }

  /**
   * Method convert Any ProtocolBuffer entity class to MongoDB Document object.
   *
   * @param object : is all ProtocolBuffer entity POJO.
   * @return Document : is a MongoDB Object
   * @throws InvalidProtocolBufferException
   */
  private Document convertObjectToDocument(MessageOrBuilder object)
      throws InvalidProtocolBufferException {

    String json = ModelDBUtils.getStringFromProtoObject(object);
    return Document.parse(json);
  }

  @Override
  public void insertOne(MessageOrBuilder object) throws InvalidProtocolBufferException {
    MongoCollection<Document> collection = this.database.getCollection(collectionName);
    Document document = convertObjectToDocument(object);
    collection.insertOne(document);
  }

  @Override
  public void insertOne(Object object) throws InvalidProtocolBufferException {
    MongoCollection<Document> collection = this.database.getCollection(collectionName);
    Document document = (Document) object;
    collection.insertOne(document);
  }

  @Override
  public List<Document> find() {
    MongoCollection<Document> collection = this.database.getCollection(collectionName);
    return collection.find().into(new ArrayList<Document>());
  }

  @Override
  public List<Document> findListByKey(
      String key,
      String value,
      Integer pageNumber,
      Integer pageLimit,
      String order,
      String sortBy) {
    MongoCollection<Document> collection = this.database.getCollection(collectionName);

    Document queryObj = new Document();
    queryObj.put(key, value);

    order = (order == null || order.isEmpty()) ? ModelDBConstants.ORDER_DESC : order;
    sortBy = (sortBy == null || sortBy.isEmpty()) ? ModelDBConstants.DATE_CREATED : sortBy;

    Document filter = new Document();
    if (order.equalsIgnoreCase(ModelDBConstants.ORDER_ASC)) {
      filter.append(sortBy, 1);
    } else {
      filter.append(sortBy, -1);
    }

    if (pageNumber == null || pageLimit == null) {
      return collection.find(queryObj).sort(filter).into(new ArrayList<Document>());
    }

    // Calculate number of documents to skip
    Integer skips = pageLimit * (pageNumber - 1);
    return collection
        .find(queryObj)
        .skip(skips)
        .limit(pageLimit)
        .sort(filter)
        .into(new ArrayList<Document>());
  }

  @Override
  public Document findByKey(String key, String value) {
    MongoCollection<Document> collection = this.database.getCollection(collectionName);
    return collection.find(eq(key, value)).first();
  }

  @Override
  public List<Document> findListByKey(String key, String value) {
    MongoCollection<Document> collection = this.database.getCollection(collectionName);
    return collection.find(eq(key, value)).into(new ArrayList<Document>());
  }

  @Override
  public List<Document> findListByObject(
      Object queryObj, Object projectionObj, Object sortObj, Integer recordLimit) {
    MongoCollection<Document> collection = this.database.getCollection(collectionName);
    Document queryDoc = (Document) queryObj;
    Document projectionDoc = new Document();
    if (projectionObj != null) {
      projectionDoc = (Document) projectionObj;
    }
    Document sortDoc = new Document();
    if (sortObj != null) {
      sortDoc = (Document) sortObj;
    }
    FindIterable<Document> documents =
        collection.find(queryDoc).projection(projectionDoc).sort(sortDoc);

    if (recordLimit != null) {
      documents = documents.limit(recordLimit);
    }

    return documents.into(new ArrayList<Document>());
  }

  @Override
  public List<Document> findListByAggregateObject(List<?> queryObj) {
    MongoCollection<Document> collection = this.database.getCollection(collectionName);
    List<Document> queryDoc = (List<Document>) queryObj;
    return collection.aggregate(queryDoc).into(new ArrayList<Document>());
  }

  @Override
  public Document findByObject(Object queryObj) {
    MongoCollection<Document> collection = this.database.getCollection(collectionName);
    Document queryDoc = (Document) queryObj;
    return collection.find(queryDoc).first();
  }

  @Override
  public Boolean deleteOne(String collectionName, String key, String value) {
    MongoCollection<Document> collection = this.database.getCollection(collectionName);
    DeleteResult deleteResult = collection.deleteOne(new Document(key, value));
    return deleteResult.wasAcknowledged();
  }

  @Override
  public long updateOne(String key, String value, MessageOrBuilder newObject)
      throws InvalidProtocolBufferException {
    MongoCollection<Document> collection = this.database.getCollection(collectionName);
    Document document = convertObjectToDocument(newObject);
    UpdateResult updateResult =
        collection.updateOne(eq(key, value), new Document("$set", document));
    return updateResult.getModifiedCount();
  }

  @Override
  public long updateOne(Object queryObj, Object updateObj) throws InvalidProtocolBufferException {
    MongoCollection<Document> collection = this.database.getCollection(collectionName);
    Document queryDocument = (Document) queryObj;
    Document updateDocument = (Document) updateObj;
    UpdateResult updateResult = collection.updateOne(queryDocument, updateDocument);
    return updateResult.getModifiedCount();
  }
}
