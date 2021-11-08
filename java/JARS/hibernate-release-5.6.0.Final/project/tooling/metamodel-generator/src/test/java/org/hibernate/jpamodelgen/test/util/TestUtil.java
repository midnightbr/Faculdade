/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.test.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import javax.persistence.metamodel.ListAttribute;
import javax.persistence.metamodel.SetAttribute;
import javax.tools.Diagnostic;

import org.jboss.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Hardy Ferentschik
 */
public class TestUtil {

	private static final Logger log = Logger.getLogger( TestUtil.class );

	private static final String PATH_SEPARATOR = File.separator;
	public static final String RESOURCE_SEPARATOR = "/";
	private static final String PACKAGE_SEPARATOR = ".";
	private static final String META_MODEL_CLASS_POSTFIX = "_";
	private static final File OUT_BASE_DIR;

	static {
		File targetDir = getTargetDir();
		OUT_BASE_DIR = new File( targetDir, "processor-generated-test-classes" );
		if ( !OUT_BASE_DIR.exists() ) {
			if ( !OUT_BASE_DIR.mkdirs() ) {
				fail( "Unable to create test output directory " + OUT_BASE_DIR.toString() );
			}
		}
	}

	private TestUtil() {
	}

	public static void assertNoSourceFileGeneratedFor(Class<?> clazz) {
		assertNotNull( "Class parameter cannot be null", clazz );
		File sourceFile = getMetaModelSourceFileFor( clazz );
		assertFalse( "There should be no source file: " + sourceFile.getName(), sourceFile.exists() );
	}

	public static void assertAbsenceOfFieldInMetamodelFor(Class<?> clazz, String fieldName) {
		assertAbsenceOfFieldInMetamodelFor(
				clazz,
				fieldName,
				"'" + fieldName + "' should not appear in metamodel class"
		);
	}

	public static void assertAbsenceOfFieldInMetamodelFor(Class<?> clazz, String fieldName, String errorString) {
		assertFalse( buildErrorString( errorString, clazz ), hasFieldInMetamodelFor( clazz, fieldName ) );
	}

	public static void assertPresenceOfFieldInMetamodelFor(Class<?> clazz, String fieldName) {
		assertPresenceOfFieldInMetamodelFor(
				clazz,
				fieldName,
				"'" + fieldName + "' should appear in metamodel class"
		);
	}

	public static void assertPresenceOfFieldInMetamodelFor(Class<?> clazz, String fieldName, String errorString) {
		assertTrue( buildErrorString( errorString, clazz ), hasFieldInMetamodelFor( clazz, fieldName ) );
	}

	public static void assertAttributeTypeInMetaModelFor(Class<?> clazz, String fieldName, Class<?> expectedType, String errorString) {
		Field field = getFieldFromMetamodelFor( clazz, fieldName );
		assertNotNull( "Cannot find field '" + fieldName + "' in " + clazz.getName(), field );
		ParameterizedType type = (ParameterizedType) field.getGenericType();
		Type actualType = type.getActualTypeArguments()[1];
		if ( expectedType.isArray() ) {
			expectedType = expectedType.getComponentType();
			actualType = getComponentType( actualType );
		}
		assertEquals(
				"Types do not match: " + buildErrorString( errorString, clazz ),
				expectedType,
				actualType
		);
	}

	public static void assertAttributeTypeInMetaModelFor(Class<?> clazz, String fieldName, Type expectedType, String errorString) {
		Field field = getFieldFromMetamodelFor( clazz, fieldName );
		assertNotNull( "Cannot find field '" + fieldName + "' in " + clazz.getName(), field );
		ParameterizedType type = (ParameterizedType) field.getGenericType();
		Type actualType = type.getActualTypeArguments()[1];
		assertEquals(
				"Types do not match: " + buildErrorString( errorString, clazz ),
				expectedType,
				actualType
		);
	}

	public static void assertSetAttributeTypeInMetaModelFor(Class<?> clazz, String fieldName, Class<?> expectedType, String errorString) {
		assertCollectionAttributeTypeInMetaModelFor( clazz, fieldName, SetAttribute.class, expectedType, errorString );
	}

	public static void assertListAttributeTypeInMetaModelFor(Class<?> clazz, String fieldName, Class<?> expectedType, String errorString) {
		assertCollectionAttributeTypeInMetaModelFor( clazz, fieldName, ListAttribute.class, expectedType, errorString );
	}

	public static void assertMapAttributesInMetaModelFor(Class<?> clazz, String fieldName, Class<?> expectedMapKey, Class<?> expectedMapValue, String errorString) {
		Field field = getFieldFromMetamodelFor( clazz, fieldName );
		assertNotNull( field );
		ParameterizedType type = (ParameterizedType) field.getGenericType();
		Type actualMapKeyType = type.getActualTypeArguments()[1];
		assertEquals( buildErrorString( errorString, clazz ), expectedMapKey, actualMapKeyType );

		Type actualMapKeyValue = type.getActualTypeArguments()[2];
		assertEquals( buildErrorString( errorString, clazz ), expectedMapValue, actualMapKeyValue );
	}

	public static void assertSuperClassRelationShipInMetamodel(Class<?> entityClass, Class<?> superEntityClass) {
		Class<?> clazz = getMetamodelClassFor( entityClass );
		Class<?> superClazz = getMetamodelClassFor( superEntityClass );
		assertEquals(
				"Entity " + superClazz.getName() + " should be the superclass of " + clazz.getName(),
				superClazz.getName(),
				clazz.getSuperclass().getName()
		);
	}

	public static void assertNoCompilationError(List<Diagnostic<?>> diagnostics) {
		for ( Diagnostic<?> diagnostic : diagnostics ) {
			if ( diagnostic.getKind().equals( Diagnostic.Kind.ERROR ) ) {
				fail( "There was a compilation error during annotation processing:\n" + diagnostic.getMessage( null ) );
			}
		}
	}

	/**
	 * Asserts that a metamodel class for the specified class got generated.
	 *
	 * @param clazz the class for which a metamodel class should have been generated.
	 */
	public static void assertMetamodelClassGeneratedFor(Class<?> clazz) {
		assertNotNull( getMetamodelClassFor( clazz ) );
	}

	/**
	 * Deletes recursively all files found in the output directory for the annotation processor.
	 */
	public static void deleteProcessorGeneratedFiles() {
		for ( File file : OUT_BASE_DIR.listFiles() ) {
			deleteFilesRecursive( file );
		}
	}

	/**
	 * @return the output directory for the generated source and class files.
	 */
	public static File getOutBaseDir() {
		return OUT_BASE_DIR;
	}

	/**
	 * Returns the static metamodel class for the specified entity.
	 *
	 * @param entityClass the entity for which to retrieve the metamodel class. Cannot be {@code null}.
	 *
	 * @return the static metamodel class for the specified entity.
	 */
	public static Class<?> getMetamodelClassFor(Class<?> entityClass) {
		assertNotNull( "Class parameter cannot be null", entityClass );
		String metaModelClassName = entityClass.getName() + META_MODEL_CLASS_POSTFIX;
		try {
			URL outDirUrl = OUT_BASE_DIR.toURI().toURL();
			URL[] urls = new URL[1];
			urls[0] = outDirUrl;
			URLClassLoader classLoader = new URLClassLoader( urls, TestUtil.class.getClassLoader() );
			return classLoader.loadClass( metaModelClassName );
		}
		catch ( Exception e ) {
			fail( metaModelClassName + " was not generated." );
		}
		// keep the compiler happy
		return null;
	}

	public static File getMetaModelSourceFileFor(Class<?> clazz) {
		String metaModelClassName = clazz.getName() + META_MODEL_CLASS_POSTFIX;
		// generate the file name
		String fileName = metaModelClassName.replace( PACKAGE_SEPARATOR, PATH_SEPARATOR );
		fileName = fileName.concat( ".java" );
		return new File( OUT_BASE_DIR + PATH_SEPARATOR + fileName );
	}

	public static String getMetaModelSourceAsString(Class<?> clazz) {
		File sourceFile = getMetaModelSourceFileFor( clazz );
		StringBuilder contents = new StringBuilder();

		try {
			BufferedReader input = new BufferedReader( new FileReader( sourceFile ) );
			try {
				String line;
				/*
						* readLine is a bit quirky :
						* it returns the content of a line MINUS the newline.
						* it returns null only for the END of the stream.
						* it returns an empty String if two newlines appear in a row.
						*/
				while ( ( line = input.readLine() ) != null ) {
					contents.append( line );
					contents.append( System.lineSeparator() );
				}
			}
			finally {
				input.close();
			}
		}
		catch ( IOException ex ) {
			ex.printStackTrace();
		}

		return contents.toString();
	}

	public static void dumpMetaModelSourceFor(Class<?> clazz) {
		log.info( "Dumping meta model source for " + clazz.getName() + ":" );
		log.info( getMetaModelSourceAsString( clazz ) );
	}

	public static Field getFieldFromMetamodelFor(Class<?> entityClass, String fieldName) {
		Class<?> metaModelClass = getMetamodelClassFor( entityClass );
		Field field;
		try {
			field = metaModelClass.getDeclaredField( fieldName );
		}
		catch ( NoSuchFieldException e ) {
			field = null;
		}
		return field;
	}

	public static String fcnToPath(String fcn) {
		return fcn.replace( PACKAGE_SEPARATOR, RESOURCE_SEPARATOR );
	}

	private static boolean hasFieldInMetamodelFor(Class<?> clazz, String fieldName) {
		return getFieldFromMetamodelFor( clazz, fieldName ) != null;
	}

	private static String buildErrorString(String baseError, Class<?> clazz) {
		StringBuilder builder = new StringBuilder();
		builder.append( baseError );
		builder.append( ".\n\n" );
		builder.append( "Source code for " );
		builder.append( clazz.getName() );
		builder.append( "_.java:" );
		builder.append( "\n" );
		builder.append( getMetaModelSourceAsString( clazz ) );
		return builder.toString();
	}

	private static Type getComponentType(Type actualType) {
		if ( actualType instanceof Class ) {
			Class<?> clazz = (Class<?>) actualType;
			if ( clazz.isArray() ) {
				return clazz.getComponentType();
			}
			else {
				fail( "Unexpected component type" );
			}
		}

		if ( actualType instanceof GenericArrayType ) {
			return ( (GenericArrayType) actualType ).getGenericComponentType();
		}
		else {
			fail( "Unexpected component type" );
			return null;
		}
	}

	private static class MetaModelFilenameFilter implements FileFilter {
		@Override
		public boolean accept(File pathName) {
			if ( pathName.isDirectory() ) {
				return true;
			}
			else {
				return pathName.getAbsolutePath().endsWith( "_.java" )
						|| pathName.getAbsolutePath().endsWith( "_.class" );
			}
		}
	}

	private static void deleteFilesRecursive(File file) {
		if ( file.isDirectory() ) {
			for ( File c : file.listFiles() ) {
				deleteFilesRecursive( c );
			}
		}
		if ( !file.delete() ) {
			fail( "Unable to delete file: " + file );
		}
	}

	/**
	 * Returns the target directory of the build.
	 *
	 * @return the target directory of the build
	 */
	public static File getTargetDir() {
		ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
		// get a URL reference to something we now is part of the classpath (our own classes)
		String currentTestClass = TestUtil.class.getName();
		int hopsToCompileDirectory = currentTestClass.split( "\\." ).length;
		int hopsToTargetDirectory = hopsToCompileDirectory + 2;
		URL classURL = contextClassLoader.getResource( currentTestClass.replace( '.', '/' ) + ".class" );
		// navigate back to '/target'
		File targetDir = new File( classURL.getFile() );
		// navigate back to '/target'
		for ( int i = 0; i < hopsToTargetDirectory; i++ ) {
			targetDir = targetDir.getParentFile();
		}
		return targetDir;
	}

	private static void assertCollectionAttributeTypeInMetaModelFor(
			Class<?> clazz,
			String fieldName,
			Class<?> collectionType,
			Class<?> expectedType,
			String errorString) {
		Field field = getFieldFromMetamodelFor( clazz, fieldName );
		assertNotNull( "Cannot find field '" + fieldName + "' in " + clazz.getName(), field );
		ParameterizedType type = (ParameterizedType) field.getGenericType();
		Type rawType = type.getRawType();

		assertEquals(
				"Types do not match: " + buildErrorString( errorString, clazz ),
				collectionType,
				rawType
		);

		Type genericType = type.getActualTypeArguments()[1];

		assertEquals(
				"Types do not match: " + buildErrorString( errorString, clazz ),
				expectedType,
				genericType
		);
	}

}


