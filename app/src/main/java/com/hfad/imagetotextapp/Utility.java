package com.hfad.imagetotextapp;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class Utility {
    static CollectionReference getCollectionReferenceForScanTexts() {
        return FirebaseFirestore.getInstance().collection("my_scans");
    }
}
