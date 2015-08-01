import sys 
import threading as t
import struct

# An list of byte arrays
list_of_lists_of_similarities = list()
ELEMENT_THRESHOLD = 5
NUM_OF_SIMS_THRESHOLD = 1

def make_bytearrays():
    """ """
    file = sys.stdin
    bytein = bytearray()
    try:
        byte = file.read(1)
        while byte != "":
            bytein.append(struct.unpack('B', byte)[0])
            byte = file.read(1)            
    finally:
        file.close()
    return bytein
        
        
def compare_bytearrays(bytearray1, bytearray2):    
    """ """
    similarities = 0
    for i in range(bytearray1.length - 1):
        if (bytearray2[i] != None and bytearray1[i] != None and 
            abs(bytearray1[i] - bytearray2[i]) < ELEMENT_THRESHOLD):
            similarities += 1
    return (similarities >= NUM_OF_SIMS_THRESHOLD)        
    
def compare_similarity_lists(bytearray):
    similar = False
    for list_of_similarities in list_of_lists_of_similarities:
        for list in list_of_similarities:
            similar = compare_bytearrays(bytearray, 
            list_of_lists_of_similarities[list_of_similarities[list]])
            if similar:
                list_of_similarities.append(bytearray)
    if not similar:        
        list_of_lists_of_similarities.append(bytearray)
                
                
def print_output():
    for list in list_of_lists_of_similarities:
        print("Similar Bytearrays:")
        for bytes in list:
            i = 0
            if i < bytes.length and i < 10:
                print("{>5}".format(bytes), end='')
                i += 1  
                if i >= 10:
                    i = 0
def Main():
    """ """
    while True:
        byteflow = make_bytearrays()
        compare_similarity_lists(byteflow)
        print_output()
        
Main()