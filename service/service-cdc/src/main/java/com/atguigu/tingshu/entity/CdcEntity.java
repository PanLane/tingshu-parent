package com.atguigu.tingshu.entity;

import lombok.Data;

import javax.persistence.Id;
import javax.persistence.Table;

@Data
@Table(name = "album_info")
public class CdcEntity {
    @Id
    private Long id;
}
