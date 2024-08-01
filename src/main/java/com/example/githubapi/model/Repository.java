package com.example.githubapi.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class Repository {
    private String name;
    private String ownerLogin;
    private List<Branch> branches;

}
