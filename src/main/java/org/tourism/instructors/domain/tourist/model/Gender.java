package org.tourism.instructors.domain.tourist.model;

public enum Gender {
    MALE {
        @Override
        public String toString () {
            return "мужской";
        }
    },
    FEMALE {
        @Override
        public String toString () {
            return "женский";
        }
    }
}
