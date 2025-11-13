We are building a UML-based domain model. 
We will describe the model in markdown files under the directory doc/domainmodel. 
We'll use the Mermaid notation.
Ensure that we always specify the multiplicity of all associations.
We will also type the attributes.
Avoid composition and aggregation unless it is an obvious composition where containment is absolute. 

Avoid directed associations, that is, do not assume that you have a link between the objects (we operate on a higher level of abstraction), e.g.:
Dont do Item "1" --> "1" ShoppingCart
insted do Item "*" -- "1" ShoppingCart
Notice that that means a item is always associated with a shopping cart. A shopping cart can have many items.