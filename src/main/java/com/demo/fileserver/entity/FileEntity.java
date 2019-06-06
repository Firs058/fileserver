package com.demo.fileserver.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

/**
 * @author firs
 */
@EqualsAndHashCode
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(fluent = true)
@Entity
@Table(name = "files")
public class FileEntity implements Serializable {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    @Column(name = "id", updatable = false, nullable = false)
    public UUID id;
    @Column(columnDefinition = "text", nullable = false)
    public String path;
    @Column(columnDefinition = "text", nullable = false)
    public String node;
    @Column(name = "file_name", columnDefinition = "text", nullable = false)
    public String fileName;
    @Column(name = "mime_type", columnDefinition = "text", nullable = false)
    public String mimeType;
    @Column(name = "file_size", columnDefinition = "text", nullable = false)
    public Long fileSize;
    @CreationTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "date_added", columnDefinition = "timestamp with time zone", nullable = false)
    public Date dateAdded;
    @Transient
    private String savedName;

    @PostLoad
    private void onLoad() {
        this.savedName = this.id + "_" + this.fileName;
    }
}
