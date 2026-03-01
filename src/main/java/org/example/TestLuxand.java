package org.example;

import Services.LuxandFaceService;

public class TestLuxand {
    public static void main(String[] args) throws Exception {
        LuxandFaceService api = new LuxandFaceService();

        var uuids = api.listPersonUuids();
        System.out.println("Found persons = " + uuids.size());

        for (String id : uuids) {
            System.out.println("Deleting: " + id);
            api.deletePerson(id);
        }

        System.out.println("DONE ✅");
    }
}