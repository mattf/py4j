'''
Created on Dec 17, 2009

@author: barthelemy
'''
from multiprocessing.process import Process
from py4j.java_gateway import JavaGateway
import logging
import subprocess
import time
import unittest


def start_echo_server():
    subprocess.call(["java", "-cp", "../../../py4j/bin/", "py4j.examples.ExampleApplication"])
    
    
def start_echo_server_process():
    # XXX DO NOT FORGET TO KILL THE PROCESS IF THE TEST DOES NOT SUCCEED
    p = Process(target=start_echo_server)
#    p.daemon = True
    p.start()
    return p

def get_list(count):
    return [unicode(i) for i in range(count)]

class Test(unittest.TestCase):

    def setUp(self):
        logger = logging.getLogger("py4j")
        logger.setLevel(logging.DEBUG)
        logger.addHandler(logging.StreamHandler())
        self.p = start_echo_server_process()
        time.sleep(1)
        self.gateway = JavaGateway()
        
    def tearDown(self):
        self.p.terminate()
        self.gateway.comm_channel.shutdown()
        time.sleep(1)
        
    def testJavaListProtocol(self):
        ex = self.gateway.getNewExample()
        pList = get_list(3)
        jList = ex.getList(3)
        pList.append(u'1')
        jList.append(u'1')
        pList.sort()
        jList.sort()
        self.assertEqual(len(pList), len(jList))
        self.assertEqual(str(pList), str(jList))
        pList.reverse()
        jList.reverse()
        self.assertEqual(len(pList), len(jList))
        self.assertEqual(str(pList), str(jList))
        
    def testJavaListSlice(self):
        ex = self.gateway.getNewExample()
        pList = get_list(5)
        jList = ex.getList(5)
        
        del pList[1:3]
        del jList[1:3]
        self.assertEqual(len(pList), len(jList))
        self.assertEqual(str(pList), str(jList))
        
    def testJavaList(self):
        ex = self.gateway.getNewExample()
        pList = get_list(3)
        jList = ex.getList(3)
        pList2 = get_list(3)
        jList2 = ex.getList(3)
        
        # Lists are not "hashable" in Python. Too bad.
        #self.assertEqual(hash(pList),hash(pList2))
        self.assertEqual(hash(jList),hash(jList2))
        
        self.assertEqual(len(pList), len(jList))
        self.assertEqual(str(pList), str(jList))
        self.assertEqual(pList, pList2)
        self.assertEqual(jList, jList2)
        pList.append(u'4')
        jList.append(u'4')
        self.assertEqual(len(pList), len(jList))
        self.assertEqual(str(pList), str(jList))
        
        self.assertEqual(pList[0], jList[0])
        self.assertEqual(pList[3], jList[3])
        
        pList.extend(pList2)
        jList.extend(jList2)
        self.assertEqual(len(pList), len(jList))
        self.assertEqual(str(pList), str(jList))
        
        self.assertEqual(u'1' in pList, u'1' in jList)
        self.assertEqual(u'500' in pList, u'500' in jList)
        
        pList[0] = u'100'
        jList[0] = u'100'
        pList[3] = u'150'
        jList[3] = u'150'
        pList[-1] = u'200'
        jList[-1] = u'200'
        self.assertEqual(len(pList), len(jList))
        self.assertEqual(str(pList), str(jList))
        
        pList.insert(0,u'100')
        jList.insert(0,u'100')
        pList.insert(3,u'150')
        jList.insert(3,u'150')
        pList.insert(-1,u'200')
        jList.insert(-1,u'200')
        self.assertEqual(len(pList), len(jList))
        self.assertEqual(str(pList), str(jList))
        
        self.assertEqual(pList.pop(),jList.pop())
        self.assertEqual(len(pList), len(jList))
        self.assertEqual(str(pList), str(jList))
        
        self.assertEqual(pList.pop(-1),jList.pop(-1))
        self.assertEqual(len(pList), len(jList))
        self.assertEqual(str(pList), str(jList))
        
        self.assertEqual(pList.pop(2),jList.pop(2))
        self.assertEqual(len(pList), len(jList))
        self.assertEqual(str(pList), str(jList))
        
        del pList[0]
        del jList[0]
        del pList[-1]
        del jList[-1]
        del pList[1]
        del jList[1]
        self.assertEqual(len(pList), len(jList))
        self.assertEqual(str(pList), str(jList))
        
        pList.append(u'700')
        jList.append(u'700')
        pList.insert(0,u'700')
        jList.insert(0,u'700')
        
        pList.remove(u'700')
        jList.remove(u'700')
        self.assertEqual(len(pList), len(jList))
        self.assertEqual(str(pList), str(jList))
        
        # Catch here: Java has a remote(int) method that is not directly supported in Python.
        # This could get tricky with list of integers... Needs to think about that.
        jList.remove(0)
        del pList[0]
        self.assertEqual(len(pList), len(jList))
        self.assertEqual(str(pList), str(jList))
        
        try:
            jList[15]
            self.fail('Should Fail!')
        except IndexError:
            self.assertTrue(True)
        
        
        
        


if __name__ == "__main__":
    #import sys;sys.argv = ['', 'Test.testJavaList']
    unittest.main()