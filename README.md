# SIRS_Proj
NotIST - 2025 SIRS Project of Masters Degree in Computer Science

Authors:

André Roque (andre.tita217@gmail.com) - 103074 | Artur Ferreira - 102482 | Jade Le Roux - 112474

# Project

NotIST is a note application that allows users to create personal notes and shareable notes. It’s a local first application, meaning that notes are first stored in the local device, and are periodically synced to an external server for backups. A new functionality of NotIST is its ability to share notes with other users. NotIST is built to ensure privacy. Notes, even the ones stored locally, are always encrypted in such a way that only its owner can decrypt them. Shared notes are also encrypted, but any contributor of the notes can decrypt them. 

The data structure of any note is a JSON document with the following structure:

```json
{
  "id": 123,
  "title": "Example Document",
  "note": "This is an example document.",
  "data_created": "2022-01-01T12:00:00Z",
  "date_modified": "2022-01-02T12:00:00Z",
  "last_modified_by": 456,
  "version": 3,
  "owner": {
    "id": 456,
    "username": "john"
  },
  "editors": [
    {
      "id": 789,
      "username": "jane"
    },
    {
      "id": 1011,
      "username": "bob"
    }
  ],
  "viewers": [
    {
      "id": 1213,
      "username": "alice"
    },
    {
      "id": 1415,
      "username": "charlie"
    }
  ]
}
```


### Protection Needs

The protected document must ensure the integrity and confidentiality of the data. Only the owner of the notes can see their content and the owner of the notes can verify that they were not tampered with. Assume the notes are stored in the server. 
Ensure the following security requirements are met:
- [SR1: Confidentiality] Only the owner of the notes can see their content.
- [SR2: Integrity 1] The owner of the notes can verify they were not tampered with.
- [SR3: Integrity 2] The owner of the notes can verify if some note is missing.
- [SR4: Authentication] Only the owner of the notes can access them.


### Security challenges
#### Security challenge A
The users of NotIST want to share their notes with anyone on the web. Implement the required functionality to allow sharing a note with other users. 
Ensure the following security requirements are met:
- [SRA1: Authentication] Only authenticated and authorized users can see the content of the notes.
- [SRA2: Integrity 1] Anyone that has access to the note can verify its integrity.
- [SRA3: Integrity 2] It is possible to verify the integrity of the notes throughout their versions.  
#### Security challenge B
Some NotIST users asked a new feature to allow them to exchange highly sensitive notes. These notes can only be opened if a user owns a set of two keys. Moreover, it is assumed that some keys may be lost, as such, if any notes were written using a revoked key then such noes should not be accepted.  
Ensure the following security requirements are met:
- [SRB1: Confidentiality 1] Each note can only be read using a set of at least two keys: a personal key, and a note key. 
- [SRB2: Confidentiality 2] Implement a multi-level access (0 - public, 1 - private, 2 - confidential, 3 - top-secret). Users with higher clearance can read any document up to that level (for example, a user with confidential can read any note that is public, private and confidential but cannot read a top-secret note).
- [SRB3: Integrity] If a key is revoked, then any note signed after its revocation should not be accepted. Previous versions of the note signed when the key was valid are accepted.

---
