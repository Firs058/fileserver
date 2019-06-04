package com.demo.fileserver.model;

import com.demo.fileserver.entity.FileEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * @author firs
 */
@EqualsAndHashCode
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(fluent = true)
public class Table {
    public String idPath;
    public String filePath;
    public FileEntity fileEntity;
}
