
1) FeatureMakerMaryServer 

Changes in this class:

- Expects an already created mysql database called wiki, tested in linux
  with username and passwd.
- Creates two tables in the wiki database:
+----------------+
| Tables_in_wiki |
+----------------+
| dbselection    | 
| unreliable     | 
+----------------+

The dbselection table contains the fields:

mysql> desc dbselection;
+----------+---------+------+-----+---------+----------------+
| Field    | Type    | Null | Key | Default | Extra          |
+----------+---------+------+-----+---------+----------------+
| id       | int(11) | NO   | PRI | NULL    | auto_increment | 
| fromFile | text    | YES  |     | NULL    |                | 
| sentence | text    | YES  |     | NULL    |                | 
| features | blob    | YES  |     | NULL    |                | 
+----------+---------+------+-----+---------+----------------+

The unreliable table contains the fields:

mysql> desc unreliable;
+----------+---------+------+-----+---------+----------------+
| Field    | Type    | Null | Key | Default | Extra          |
+----------+---------+------+-----+---------+----------------+
| id       | int(11) | NO   | PRI | NULL    | auto_increment | 
| fromFile | text    | YES  |     | NULL    |                | 
| sentence | text    | YES  |     | NULL    |                | 
+----------+---------+------+-----+---------+----------------+


- Expects a list of text files, the text files from the wikipedia.
- Process every text file as follows:
  * get the sentences in each file and store them in the "sentence" field
    of dbselection or unreliable.
  * get the feature vectors of each reliable sentence, creates byte[] arrays 
    and store them in the binary blob field "features".
  * discard problematic (unreliable) sentences, these are saved in the
    unreliable table.

The following is an example of the (tiny) English corpus I was using 
for testing:

| id | fromFile        | sentence
|  1 | enwiki02.txt_1  | Climate encompasses the temperatures, humidity, rainfall, atmospheric particle count and numerous other meteorogical factors in a given region over long periods of time, as opposed to the term weather, which refers to current activity. 
|  2 | enwiki02.txt_2  | The climate of a location is affected by its latitude, terrain, altitude, persistent ice or snow cover, as well as nearby oceans and their currents.
|  3 | enwiki02.txt_3  | Climates can be classified using parameters such as temperature and rainfall to define specific climate types.

...

|  8 | enwiki03.txt_1  | Cultural geography is a sub field within human geography.     
|  9 | enwiki03.txt_2  | Cultural geography is the study of cultural products and norms and their variation across and relations to spaces and places.

                                                                                               
2) DatabaseSelector

The changes in this class are the following:

- Process all the sentences in the dbselection database.
  (maybe if the database is too big it would be better to start with
   some sentences, not sure what is the best way here, Anna was using a list of
   basenames, but I am not sure if she uses all the list at once,
   or she processed per groups?)
- Read the byte[] features directly from the database
- Create as before a init.bin file
  (not sure if you want this information in the database as well?) 
- The rest of the processing is done as Anna did it.
