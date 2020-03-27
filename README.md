# Snips Compiler Gen.2

## TODO
- Make Functions able to address parameters located in the stack, load and
edit them.


- Make Declarations aware of the stack, if datatype is > 1 Words or no reg is
free, save declaration in stack. 
Issues: Functions need check to add the fp/sp exchange
if the stacksize has changed through declarations!


- Make Algorithm to scan next code statements to determine which variable is
used least for regStack overwriting


- Build system that branches all returns to centralized pop, frame reset and bx.