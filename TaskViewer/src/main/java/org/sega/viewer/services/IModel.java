package org.sega.viewer.services;

import org.sega.viewer.models.BaseModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.io.Serializable;
import java.util.List;

/**
 * Generic interface used to process O-R mapping for models
 *
 * @author: Raysmond
 */
public interface IModel{
    <T extends BaseModel> T create(T entity);

    <T extends BaseModel> T get(Class<T> clazz, Long id);

    <T extends BaseModel> List<T> findAll(Class<T> clazz);

    // TODO
//    <T extends BaseModel> void save(T entity);
//
//    <T extends BaseModel> boolean update(T entity);
//
//    <T extends BaseModel> T delete(Long id);
}
