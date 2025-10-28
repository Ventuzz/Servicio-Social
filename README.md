

# **Backups**

El primer paso para realizar una copia de seguridad es abrir una terminal como administrador, tienes que ir a la ruta donde tienes instalado el MySQL server y dirigirte a la carpeta bin
como se observa en la siguiente imagen. 

<img width="557" height="136" alt="image" src="https://github.com/user-attachments/assets/9d91ba21-4e65-4f08-8b33-007a8ac9749c" />

Dentro de nuestra carpeta bin vamos a ir a nuestra terminar y adaptar el siguiente comando;

mysqldump -u (Ingresa tu usuario) -p (Ingresa el nombre de tu base de datos) > amudb_backup.sql (nombre que le quieres dar al backup)

-u = se remplaza por el usuario

-p = este parametro es para que te solicite tu contraseña

-ambudb = El nombre de tu database

<img width="691" height="76" alt="image" src="https://github.com/user-attachments/assets/759cb9ca-803d-45f8-94df-3a2858f249ea" />

**Comando**

" mysqldump -u admin -p ambudb > backup.sql "

Una vez que corras el comando se te pedirá tu contraseña, despues de ingresarla y presionar ENTER se generará el backup en la carpeta bin

<img width="618" height="85" alt="image" src="https://github.com/user-attachments/assets/08b1ea9f-77dd-4917-a2d8-bf3730f2351e" />

# **Cargar un backup**

Para cargar un backup se necesita abrir una terminal como administrador en la misma ruta bin dentro de la carpeta de MySQL server

<img width="549" height="104" alt="image" src="https://github.com/user-attachments/assets/183f0eb6-df67-4310-b1d0-d59b53078afa" />


Una vez ahí se tendrá que adaptar el siguiente comando;

mysql -u (Ingresa tu usuario) -p ambudb(Ingresa el nombre de tu base de datos)  < amudb_backup.sql (Nombre del archivo que quieres cargar)

-u = Se remplaza por tu usuario

-p = Se te solicitará tu contraseña

-ambudb = El nombre de tu database

ambudb_backup.sql = El nombre de tu archivo que quieres cargar


<img width="654" height="48" alt="image" src="https://github.com/user-attachments/assets/be90e571-0912-4c70-9564-11aaae106483" />

**Comando**

" mysql -u -p ambudb < amudb_backup.sql "


Una vez hecho este comando la base de datos se actualizará por la del archivo especificado.
