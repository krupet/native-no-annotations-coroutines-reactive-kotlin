CREATE TABLE IF NOT EXISTS users
(
    login varchar PRIMARY KEY,
    firstname varchar,
    lastname varchar
);

INSERT INTO users VALUES ('smaldini', 'Stéphane', 'Maldini'),('sdeleuze', 'Sébastien', 'Deleuze'),('bclozel', 'Brian', 'Clozel');