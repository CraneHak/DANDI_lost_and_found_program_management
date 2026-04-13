package org.example.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "lost_item")
public class LostItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "post_no", unique = true)
    private Integer postNo;

    @Column(name = "item_name")
    private String itemName;

    @Column(name = "found_location")
    private String foundLocation;

    @Column(name = "stored_location")
    private String storedLocation;

    @Column(name = "stored_date")
    private LocalDate storedDate;

    @Column(name = "contact")
    private String contact;

    @Column(name = "color")
    private String color;

    @Column(name = "item_type")
    private String itemType;

    @Column(name = "image_url")
    private String imageUrl;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getPostNo() { return postNo; }
    public void setPostNo(Integer postNo) { this.postNo = postNo; }

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }

    public String getFoundLocation() { return foundLocation; }
    public void setFoundLocation(String foundLocation) { this.foundLocation = foundLocation; }

    public String getStoredLocation() { return storedLocation; }
    public void setStoredLocation(String storedLocation) { this.storedLocation = storedLocation; }

    public LocalDate getStoredDate() { return storedDate; }
    public void setStoredDate(LocalDate storedDate) { this.storedDate = storedDate; }

    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getItemType() { return itemType; }
    public void setItemType(String itemType) { this.itemType = itemType; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}
